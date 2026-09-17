package com.mengy.tools.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.CommunityArticleDetailDTO;
import com.mengy.tools.dto.CommunityPostDTO;
import com.mengy.tools.dto.CommunityPostInsert;
import com.mengy.tools.dto.CommunityUserStateDTO;
import com.mengy.tools.dto.PostCreateRequest;
import com.mengy.tools.mapper.CommunityArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 社区用户发帖服务。
 *
 * 与站主博客的关系：内容同表（blog_article），用 content_type 区分：
 *   blog      = 站主博客（门户 /blog 展示）
 *   community = 社区用户帖（社区展示；门户查询已加类型过滤，不会混入）
 *
 * 安全与治理：
 *   1. 正文按 Markdown 存储，HTML 由 MarkdownService 渲染并白名单消毒后入库；
 *   2. 发帖限频（60 秒 1 篇 / 每天 10 篇）由 CommunityGuardService 执行；
 *   3. 新用户、含链接、命中敏感词的内容初始为「待审」（status=0），审核通过才公开；
 *   4. 仅作者本人可编辑/删除自己的帖子；治理动作写入操作日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityPostService {

    /** 标题长度 */
    private static final int TITLE_MIN = 2;
    private static final int TITLE_MAX = 100;
    /** 正文长度（Markdown 字符数） */
    private static final int CONTENT_MIN = 10;
    private static final int CONTENT_MAX = 50_000;
    /** 摘要自动截断长度 */
    private static final int SUMMARY_LEN = 150;
    /** 社区图片上传：大小与格式 */
    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_IMG_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private final CommunityGuardService guard;
    private final MarkdownService markdownService;
    private final CommunityAuditService auditService;
    private final CommunityArticleMapper articleMapper;

    @Value("${mengy.upload.dir}")
    private String uploadDir;

    // ==================== 发帖 ====================

    /** 创建帖子，返回新帖 id */
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(Long userId, PostCreateRequest req) {
        CommunityUserStateDTO user = guard.loadUser(userId);
        guard.assertCanWrite(user);
        // 先做参数校验再消耗限频令牌：否则一次非法提交会把用户锁 60 秒
        String title = normalizeTitle(req.getTitle());
        String markdown = normalizeContent(req.getContent());
        guard.checkPostRate(userId);
        String html = markdownService.renderToSafeHtml(markdown);

        CommunityPostInsert post = new CommunityPostInsert();
        post.setTitle(title);
        post.setSummary(buildSummary(req.getSummary(), markdown));
        post.setContent(markdown);
        post.setContentHtml(html);
        post.setCover(trimToEmpty(req.getCover()));
        post.setCategoryId(req.getCategoryId());
        post.setAuthorId(userId);
        post.setAllowComment(req.getAllowComment() == null || req.getAllowComment() == 1 ? 1 : 0);
        // 审核判定：新用户 / 含链接 / 命中敏感词 → 待审
        post.setStatus(guard.decideInitialStatus(user, markdown));

        articleMapper.insertPost(post);
        log.info("社区发帖：id={} author={} status={} title={}", post.getId(), userId, post.getStatus(), title);
        return post.getId();
    }

    /** 编辑自己的帖子（内容重新渲染消毒；若新内容触发待审规则则回到待审） */
    @Transactional(rollbackFor = Exception.class)
    public void updateOwnPost(Long userId, Long postId, PostCreateRequest req) {
        CommunityUserStateDTO user = guard.loadUser(userId);
        guard.assertCanWrite(user);

        CommunityPostDTO brief = articleMapper.selectOwnPostBrief(postId);
        if (brief == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或不属于你");
        }
        String title = normalizeTitle(req.getTitle());
        String markdown = normalizeContent(req.getContent());

        CommunityPostInsert post = new CommunityPostInsert();
        post.setId(postId);
        post.setAuthorId(userId);
        post.setTitle(title);
        post.setSummary(buildSummary(req.getSummary(), markdown));
        post.setContent(markdown);
        post.setContentHtml(markdownService.renderToSafeHtml(markdown));
        post.setCover(trimToEmpty(req.getCover()));
        post.setCategoryId(req.getCategoryId());
        post.setAllowComment(req.getAllowComment() == null || req.getAllowComment() == 1 ? 1 : 0);
        // 已发布的内容再次触发待审规则（新用户/含链接/敏感词）时回到待审，避免“先发后改”绕过审核
        int decided = guard.decideInitialStatus(user, markdown);
        post.setStatus(decided == 0 ? 0 : (brief.getStatus() == null ? 1 : brief.getStatus()));

        int affected = articleMapper.updateOwnPost(post);
        if (affected == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能编辑自己的帖子");
        }
    }

    /** 删除自己的帖子（软删） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnPost(Long userId, Long postId) {
        CommunityPostDTO brief = articleMapper.selectOwnPostBrief(postId);
        if (brief == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或不属于你");
        }
        int affected = articleMapper.softDeleteOwnPost(postId, userId);
        if (affected == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的帖子");
        }
    }

    /** 我的帖子（含待审/已屏蔽） */
    public IPage<CommunityPostDTO> myPosts(Long userId, int page, int size, Integer status) {
        return articleMapper.selectMyPosts(new Page<>(Math.max(page, 1), clamp(size)), userId, status);
    }

    /** 作者编辑页回填（含 Markdown 原文，不限状态） */
    public CommunityArticleDetailDTO ownPostDetail(Long userId, Long postId) {
        CommunityArticleDetailDTO dto = articleMapper.selectOwnPostDetail(postId, userId);
        if (dto == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或不属于你");
        }
        return dto;
    }

    // ==================== 后台治理 ====================

    public IPage<CommunityPostDTO> adminPosts(int page, int size, Integer status, String keyword) {
        return articleMapper.selectAdminPostPage(new Page<>(Math.max(page, 1), clamp(size)),
                status, (keyword == null || keyword.isBlank()) ? null : keyword.trim());
    }

    /** 帖文审核：0待审 1已发布 2已屏蔽 */
    @Transactional(rollbackFor = Exception.class)
    public void auditPost(Long operatorId, String operatorName, String ip, Long postId, Integer status, String note) {
        if (status == null || status < 0 || status > 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态取值非法");
        }
        CommunityPostDTO brief = articleMapper.selectOwnPostBrief(postId);
        if (brief == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        int before = brief.getStatus() == null ? -1 : brief.getStatus();
        articleMapper.updatePostStatus(postId, status);
        auditService.write(operatorId, operatorName, "post.audit", "post", postId,
                "status=" + before, "status=" + status, note, ip);
    }

    /** 后台删除帖子（软删） */
    @Transactional(rollbackFor = Exception.class)
    public void deletePostByAdmin(Long operatorId, String operatorName, String ip, Long postId, String note) {
        CommunityPostDTO brief = articleMapper.selectOwnPostBrief(postId);
        if (brief == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        articleMapper.softDeletePostByAdmin(postId);
        auditService.write(operatorId, operatorName, "post.delete", "post", postId,
                "deleted=0", "deleted=1", note, ip);
    }

    // ==================== 图片上传（社区专用） ====================

    /**
     * 社区图片上传：登录 + 未禁言 + 限频 + 类型/大小校验。
     * 不复用管理端上传接口（那个需要 article:edit 权限），普通用户不该拿到管理端权限点。
     */
    public String uploadImage(Long userId, MultipartFile file) {
        guard.assertCanWrite(guard.loadUser(userId));
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_IMG_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 jpg/jpeg/png/gif/webp/bmp 图片");
        }
        guard.checkUploadRate(userId);
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(dir.resolve(filename));
            return "/images/" + filename;
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "图片保存失败：" + e.getMessage());
        }
    }

    // ==================== 内部工具 ====================

    private static String normalizeTitle(String title) {
        String t = title == null ? "" : title.trim();
        if (t.length() < TITLE_MIN) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标题至少 " + TITLE_MIN + " 个字");
        }
        if (t.length() > TITLE_MAX) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标题最多 " + TITLE_MAX + " 个字");
        }
        return t;
    }

    private static String normalizeContent(String content) {
        String c = content == null ? "" : content.replace("\r\n", "\n").trim();
        if (c.length() < CONTENT_MIN) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "正文至少 " + CONTENT_MIN + " 个字");
        }
        if (c.length() > CONTENT_MAX) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "正文最多 " + CONTENT_MAX + " 个字");
        }
        return c;
    }

    private String buildSummary(String summary, String markdown) {
        String s = summary == null ? "" : summary.trim();
        if (!s.isEmpty()) {
            return s.length() > 500 ? s.substring(0, 500) : s;
        }
        return markdownService.toPlainText(markdown, SUMMARY_LEN);
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static int clamp(int size) {
        return Math.min(Math.max(size, 1), 50);
    }
}
