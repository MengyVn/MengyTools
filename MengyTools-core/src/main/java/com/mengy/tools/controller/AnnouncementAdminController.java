package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mengy.tools.common.Result;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.dto.AnnouncementDetailDTO;
import com.mengy.tools.dto.AnnouncementListItemDTO;
import com.mengy.tools.entity.Announcement;
import com.mengy.tools.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 后台公告管理接口（需鉴权 + 权限校验）。
 * 权限标识与 sys_menu 中 announce:list/add/edit/delete 对齐。
 * 管理端可见所有状态（草稿/已发布/定时/已下线），与公开端 AnnouncementPortalController 区分。
 */
@RestController
@RequestMapping("/api/v1/admin/announcements")
@RequiredArgsConstructor
public class AnnouncementAdminController {

    private final AnnouncementMapper announcementMapper;

    /**
     * 公告分页列表（管理端，支持按标题/状态筛选，可见所有状态）。
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('announce:list')")
    public Result<IPage<AnnouncementListItemDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status) {
        Page<Announcement> p = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getIsMarquee)
                .orderByDesc(Announcement::getIsPersistent)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getCreateTime);
        if (title != null && !title.isBlank()) {
            wrapper.like(Announcement::getTitle, title);
        }
        if (status != null) {
            wrapper.eq(Announcement::getStatus, status);
        }
        IPage<Announcement> annPage = announcementMapper.selectPage(p, wrapper);
        IPage<AnnouncementListItemDTO> result = annPage.convert(a -> {
            AnnouncementListItemDTO dto = new AnnouncementListItemDTO();
            BeanUtils.copyProperties(a, dto);
            return dto;
        });
        return Result.ok(result);
    }

    /**
     * 公告详情（管理端，含 content，可见所有状态）。
     */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('announce:list')")
    public Result<AnnouncementDetailDTO> get(@PathVariable Long id) {
        Announcement ann = announcementMapper.selectById(id);
        if (ann == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        AnnouncementDetailDTO dto = new AnnouncementDetailDTO();
        BeanUtils.copyProperties(ann, dto);
        return Result.ok(dto);
    }

    /**
     * 新增公告（草稿/立即发布/定时发布）。
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('announce:add')")
    public Result<Long> create(@RequestBody AnnouncementCreateDTO dto) {
        Announcement ann = new Announcement();
        BeanUtils.copyProperties(dto, ann);
        normalize(ann);
        announcementMapper.insert(ann);
        return Result.ok(ann.getId());
    }

    /**
     * 修改公告。
     */
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('announce:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody AnnouncementCreateDTO dto) {
        Announcement exists = announcementMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        Announcement ann = new Announcement();
        BeanUtils.copyProperties(dto, ann);
        ann.setId(id);
        normalize(ann);
        announcementMapper.updateById(ann);
        return Result.ok();
    }

    /**
     * 删除公告（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('announce:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        announcementMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 公告创建/编辑统一规范化逻辑：
     * - contentFormat 缺省回退 markdown
     * - status 缺省回退 0(草稿)
     * - isPersistent 缺省回退 0
     * - isMarquee 缺省回退 0
     * - displayDuration 缺省回退 0；非跑马灯时强制 0
     * - status=1 且 publishTime 为空 → 立即发布(补 publishTime=now)
     * - status=1 且 publishTime 在未来 → 自动改为 2(定时中)
     * - viewCount 缺省回退 0
     */
    private void normalize(Announcement ann) {
        if (ann.getContentFormat() == null || ann.getContentFormat().isBlank()) {
            ann.setContentFormat("markdown");
        }
        if (ann.getStatus() == null) {
            ann.setStatus(0);
        }
        if (ann.getIsPersistent() == null) {
            ann.setIsPersistent(0);
        }
        if (ann.getIsMarquee() == null) {
            ann.setIsMarquee(0);
        }
        if (ann.getDisplayDuration() == null) {
            ann.setDisplayDuration(0);
        }
        // 非跑马灯公告，displayDuration 无意义
        if (ann.getIsMarquee() != 1) {
            ann.setDisplayDuration(0);
        }
        if (ann.getViewCount() == null) {
            ann.setViewCount(0);
        }
        // 已发布且未指定发布时间 → 立即发布
        if (ann.getStatus() == 1 && ann.getPublishTime() == null) {
            ann.setPublishTime(LocalDateTime.now());
        }
        // 已发布但发布时间在未来 → 转为定时中
        if (ann.getStatus() == 1 && ann.getPublishTime() != null
                && ann.getPublishTime().isAfter(LocalDateTime.now())) {
            ann.setStatus(2);
        }
    }

    /**
     * 公告创建/编辑请求体。
     * 前端回传时间格式与详情接口一致（yyyy-MM-dd HH:mm:ss），必须注解否则 Jackson 按 ISO 解析报 500。
     */
    @lombok.Data
    public static class AnnouncementCreateDTO {
        private String title;
        private String content;
        /** 内容格式：markdown / html，默认 markdown */
        private String contentFormat;
        /** 是否常驻：1常驻(每次登录都弹出) 0非常驻(关闭后跨登录保持) */
        private Integer isPersistent;
        /** 是否滚动出现在首页顶部跑马灯：1是 0否 */
        private Integer isMarquee;
        /** 跑马灯显示时长(分钟)：0/null=一直显示直到手动关闭 */
        private Integer displayDuration;
        /** 0草稿 1已发布 2定时中 3已下线 */
        private Integer status;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime publishTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime expireTime;
    }
}
