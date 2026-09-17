package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.common.Result;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.CommunityArticleDTO;
import com.mengy.tools.dto.CommunityBoardDTO;
import com.mengy.tools.dto.CommunityArticleDetailDTO;
import com.mengy.tools.dto.CommunityCommentDTO;
import com.mengy.tools.dto.CommunitySidebarDTO;
import com.mengy.tools.dto.CommunityUserCommentDTO;
import com.mengy.tools.dto.CommunityPostDTO;
import com.mengy.tools.dto.CommunityProfileUpdateRequest;
import com.mengy.tools.dto.PostCreateRequest;
import com.mengy.tools.dto.CommunityUserProfileDTO;
import com.mengy.tools.dto.CommentCreateRequest;
import com.mengy.tools.dto.FollowRequest;
import com.mengy.tools.dto.ReactionRequest;
import com.mengy.tools.dto.ReportCreateRequest;
import com.mengy.tools.mapper.BlogCategoryMapper;
import com.mengy.tools.mapper.CommunityArticleMapper;
import com.mengy.tools.mapper.CommunityCommentMapper;
import com.mengy.tools.mapper.CommunityUserMapper;
import com.mengy.tools.entity.CommunityNotification;
import com.mengy.tools.entity.SysUser;
import com.mengy.tools.mapper.SysUserMapper;
import com.mengy.tools.service.CommunityInteractService;
import com.mengy.tools.service.CommunityPostService;
import com.mengy.tools.service.CommunityWriteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 社区前台公开只读接口（无需鉴权）。
 *
 * 路径前缀 /api/v1/community，SecurityConfig 放行 GET。
 * 与门户（/api/v1/blog/**、/api/v1/nav、/api/v1/tools）并存且互不影响：
 * 门户处于冻结状态，社区页面消费本控制器。
 *
 * 写入类接口（发评论/点赞/收藏/关注/举报）在 P3 落地，届时 POST/DELETE 需登录。
 */
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
@Slf4j
public class CommunityPortalController {

    /** 单页最大条数，防止翻页参数被滥用拖库 */
    private static final int MAX_PAGE_SIZE = 50;

    /** 全文索引可用标记：一旦发现不可用（未执行迁移）就永久降级为 LIKE，避免每次请求都抛异常 */
    private volatile boolean fulltextAvailable = true;

    private final CommunityArticleMapper articleMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityWriteService writeService;
    private final CommunityInteractService interactService;
    private final SysUserMapper sysUserMapper;
    private final CommunityPostService postService;
    private final BlogCategoryMapper categoryMapper;

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static long clampPage(int page) {
        return Math.max(page, 1);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ==================== 文章 ====================

    /**
     * 文章列表：社区首页/板块页/搜索结果共用。
     *
     * @param sort latest（默认，发布时间倒序） / hot（评论数、浏览数倒序）
     */
    @GetMapping("/articles")
    public Result<IPage<CommunityArticleDTO>> articles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort) {
        Page<CommunityArticleDTO> p = new Page<>(clampPage(page), clampSize(size));
        return Result.ok(articleMapper.selectCommunityPage(p, categoryId, trimToNull(keyword), sort));
    }

    /** 文章详情（仅已发布）。阅读量由前端调用既有 /api/v1/track/view 上报，此处不自增。 */
    @GetMapping("/articles/{id}")
    public Result<CommunityArticleDetailDTO> article(@PathVariable Long id) {
        CommunityArticleDetailDTO dto = articleMapper.selectCommunityDetail(id);
        if (dto == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在或未发布");
        }
        return Result.ok(dto);
    }

    /**
     * 文章评论分页：顶层评论按楼层分页，每条顶层评论附带其全部子回复（楼中楼）。
     *
     * @param sort asc（默认，楼层正序） / desc（楼层倒序）
     */
    @GetMapping("/articles/{id}/comments")
    public Result<IPage<CommunityCommentDTO>> comments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "asc") String sort) {
        if (articleMapper.selectCommunityDetail(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在或未发布");
        }
        Page<CommunityCommentDTO> p = new Page<>(clampPage(page), clampSize(size));
        IPage<CommunityCommentDTO> result = commentMapper.selectTopPage(p, id, sort);
        fillReplies(result.getRecords());
        return Result.ok(result);
    }

    /** 批量把子回复挂到对应顶层评论上（一次查询，避免 N+1）。 */
    private void fillReplies(List<CommunityCommentDTO> tops) {
        if (tops == null || tops.isEmpty()) {
            return;
        }
        List<Long> rootIds = tops.stream().map(CommunityCommentDTO::getId).toList();
        List<CommunityCommentDTO> replies = commentMapper.selectRepliesByRootIds(rootIds);
        Map<Long, List<CommunityCommentDTO>> grouped = replies.stream()
                .collect(Collectors.groupingBy(CommunityCommentDTO::getRootId));
        for (CommunityCommentDTO top : tops) {
            List<CommunityCommentDTO> list = grouped.getOrDefault(top.getId(), List.of());
            top.setReplies(list);
            top.setReplyCount(list.size());
        }
    }

    // ==================== 用户 ====================

    /** 公开用户主页（不含登录名，仅昵称等社区身份信息）。 */
    @GetMapping("/users/{id}")
    public Result<CommunityUserProfileDTO> user(@PathVariable Long id) {
        CommunityUserProfileDTO dto = userMapper.selectPublicProfile(id);
        if (dto == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return Result.ok(dto);
    }

    /** 某用户的评论列表（带所属文章标题）。 */
    @GetMapping("/users/{id}/comments")
    public Result<IPage<CommunityUserCommentDTO>> userComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (userMapper.selectPublicProfile(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Page<CommunityUserCommentDTO> p = new Page<>(clampPage(page), clampSize(size));
        return Result.ok(commentMapper.selectUserCommentPage(p, id));
    }

    // ==================== 板块 / 搜索 / 侧栏 ====================

    /**
     * 搜索：优先走 ngram 全文索引（标题+摘要+正文，相关度排序）；
     * 若索引尚未创建（未执行 mengy_tools_community_search.sql），自动降级为标题/摘要 LIKE。
     */
    @GetMapping("/search")
    public Result<IPage<CommunityArticleDTO>> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId) {
        String keyword = trimToNull(q);
        Page<CommunityArticleDTO> p = new Page<>(clampPage(page), clampSize(size));
        if (keyword == null) {
            return Result.ok(p);
        }
        if (fulltextAvailable) {
            try {
                return Result.ok(articleMapper.selectSearchPage(p, keyword, categoryId));
            } catch (Exception e) {
                fulltextAvailable = false;
                log.warn("全文索引不可用，搜索降级为 LIKE（请确认已执行 mengy_tools_community_search.sql）：{}",
                        e.getMessage());
            }
        }
        return Result.ok(articleMapper.selectCommunityPage(p, categoryId, keyword, "latest"));
    }

    /** 侧栏聚合：站点统计 + 板块导航 + 活跃用户，一次请求喂满右侧栏。 */
    @GetMapping("/sidebar")
    public Result<CommunitySidebarDTO> sidebar() {
        CommunitySidebarDTO dto = new CommunitySidebarDTO();
        dto.setArticleCount(articleMapper.countPublished());
        dto.setCommentCount(commentMapper.countByStatus(1));
        dto.setUserCount(userMapper.countUsers());
        dto.setBoards(articleMapper.selectBoardList());
        dto.setActiveUsers(userMapper.selectActiveUsers(8));
        return Result.ok(dto);
    }

    // ==================== 写入链路（需登录，P3） ====================

    /** 发表评论 / 楼中楼回复。命中限频、敏感词、新用户规则时进入待审。 */
    @PostMapping("/comments")
    public Result<CommunityCommentDTO> createComment(@RequestBody CommentCreateRequest req,
                                                     HttpServletRequest request) {
        SysUser me = requireLogin();
        return Result.ok(writeService.createComment(me.getId(), me.getNickname(), me.getAvatar(),
                resolveClientIp(request), req));
    }

    /** 撤回自己的评论（顶层评论会连同子回复一起软删） */
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        writeService.deleteOwnComment(requireLogin().getId(), id);
        return Result.ok();
    }

    /** 点赞 / 收藏（开关式幂等） */
    @PostMapping("/reactions")
    public Result<Map<String, Object>> react(@RequestBody ReactionRequest req) {
        return Result.ok(interactService.toggleReaction(requireLogin().getId(), req));
    }

    /** 我的点赞/收藏状态（详情页初始化按钮用） */
    @GetMapping("/reactions/state")
    public Result<Map<String, Object>> reactionState(@RequestParam String targetType,
                                                     @RequestParam Long targetId) {
        return Result.ok(interactService.reactionState(requireLogin().getId(), targetType, targetId));
    }

    /** 批量互动状态（评论列表初始化点赞红心用） */
    @GetMapping("/reactions/state/batch")
    public Result<java.util.List<Long>> reactionStateBatch(@RequestParam String targetType,
                                                           @RequestParam String type,
                                                           @RequestParam java.util.List<Long> ids) {
        return Result.ok(interactService.reactedIds(requireLogin().getId(), targetType, type, ids));
    }

    /** 关注 / 取关（用户） */
    @PostMapping("/follows")
    public Result<Map<String, Object>> follow(@RequestBody FollowRequest req) {
        return Result.ok(interactService.toggleFollow(requireLogin().getId(), req));
    }

    /** 我的关注状态 + 总关注数 */
    @GetMapping("/follows/state")
    public Result<Map<String, Object>> followState(@RequestParam String targetType,
                                                   @RequestParam Long targetId) {
        return Result.ok(interactService.followState(requireLogin().getId(), targetType, targetId));
    }

    /** 更新社区资料（个人签名） */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody CommunityProfileUpdateRequest req) {
        writeService.updateSignature(requireLogin().getId(), req.getSignature());
        return Result.ok();
    }

    /** 举报评论/文章/用户 */
    @PostMapping("/reports")
    public Result<Void> report(@RequestBody ReportCreateRequest req, HttpServletRequest request) {
        SysUser me = requireLogin();
        writeService.createReport(me.getId(), me.getNickname(), resolveClientIp(request), req);
        return Result.ok();
    }

    /** 我的通知分页（unreadOnly=true 只看未读） */
    @GetMapping("/notifications")
    public Result<IPage<CommunityNotification>> notifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return Result.ok(interactService.myNotifications(requireLogin().getId(), page, size, unreadOnly));
    }

    /** 未读数（顶栏红点） */
    @GetMapping("/notifications/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(interactService.unreadCount(requireLogin().getId()));
    }

    /** 全部标记已读 */
    @PostMapping("/notifications/read")
    public Result<Integer> markRead() {
        return Result.ok(interactService.markAllRead(requireLogin().getId()));
    }

    /** 板块列表（分类即板块）：含帖子数与最近一篇，供板块页与侧栏使用 */
    @GetMapping("/boards")
    public Result<List<CommunityBoardDTO>> boards() {
        return Result.ok(articleMapper.selectBoardList());
    }

    /** 板块列表（发帖时选择板块用；与门户共用 blog_category 表） */
    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        return Result.ok(categoryMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mengy.tools.entity.BlogCategory>()
                                .orderByAsc(com.mengy.tools.entity.BlogCategory::getSort)
                                .orderByAsc(com.mengy.tools.entity.BlogCategory::getId))
                .stream()
                .map(c -> {
                    Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("id", c.getId());
                    item.put("name", c.getName());
                    item.put("slug", c.getSlug());
                    return item;
                })
                .toList());
    }

    // ==================== 发帖（需登录，P5） ====================

    /** 发表帖子：Markdown 正文服务端渲染并消毒；新用户/含链接/敏感词进待审 */
    @PostMapping("/posts")
    public Result<Map<String, Object>> createPost(@RequestBody PostCreateRequest req) {
        SysUser me = requireLogin();
        Long id = postService.createPost(me.getId(), req);
        CommunityPostDTO brief = articleMapper.selectOwnPostBrief(id);
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("id", id);
        data.put("status", brief == null ? null : brief.getStatus());
        return Result.ok(data);
    }

    /** 编辑自己的帖子 */
    @PutMapping("/posts/{id}")
    public Result<Void> updatePost(@PathVariable Long id, @RequestBody PostCreateRequest req) {
        postService.updateOwnPost(requireLogin().getId(), id, req);
        return Result.ok();
    }

    /** 删除自己的帖子（软删） */
    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        postService.deleteOwnPost(requireLogin().getId(), id);
        return Result.ok();
    }

    /** 我的帖子（含待审/已屏蔽） */
    @GetMapping("/my-posts")
    public Result<IPage<CommunityPostDTO>> myPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(postService.myPosts(requireLogin().getId(), page, size, status));
    }

    /** 编辑页回填：取自己的帖子详情（含 Markdown 原文，不限状态） */
    @GetMapping("/my-posts/{id}")
    public Result<CommunityArticleDetailDTO> myPostDetail(@PathVariable Long id) {
        return Result.ok(postService.ownPostDetail(requireLogin().getId(), id));
    }

    /** 社区图片上传（正文插图 / 封面）：登录 + 限频 + 类型与大小校验 */
    @PostMapping("/files/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = postService.uploadImage(requireLogin().getId(), file);
        return Result.ok(Map.of("url", url));
    }

    // ==================== 内部工具 ====================

    /** 取当前登录用户；未登录（或账号异常）直接 401 */
    private SysUser requireLogin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null
                || "anonymousUser".equals(auth.getName())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        SysUser user = sysUserMapper.selectByUsername(auth.getName());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        return user;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
