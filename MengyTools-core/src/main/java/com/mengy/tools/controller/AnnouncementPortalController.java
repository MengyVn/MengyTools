package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.common.Result;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.dto.AnnouncementDetailDTO;
import com.mengy.tools.dto.AnnouncementListItemDTO;
import com.mengy.tools.entity.Announcement;
import com.mengy.tools.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台门户公告公开接口（无需鉴权）。
 * 路径前缀 /api/v1/portal/announcements，SecurityConfig 放行 GET 方法。
 * 公开端仅返回 status=1 且已到发布时间的公告。
 */
@RestController
@RequestMapping("/api/v1/portal/announcements")
@RequiredArgsConstructor
public class AnnouncementPortalController {

    private final AnnouncementMapper announcementMapper;

    /**
     * 当前生效公告（常驻优先、发布时间倒序）。
     * 常驻公告永不消失；非常驻公告需未过 expire_time。
     */
    @GetMapping("/active")
    public Result<List<AnnouncementListItemDTO>> active() {
        return Result.ok(announcementMapper.selectActiveList());
    }

    /**
     * 历史公告分页（status=1 且已到发布时间，按 publish_time 倒序）。
     */
    @GetMapping
    public Result<IPage<AnnouncementListItemDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AnnouncementListItemDTO> p = new Page<>(page, size);
        return Result.ok(announcementMapper.selectPortalPage(p));
    }

    /**
     * 公告详情（含完整 content）。仅返回已发布且已到发布时间的公告，同时累加阅读量。
     */
    @GetMapping("/{id}")
    public Result<AnnouncementDetailDTO> get(@PathVariable Long id) {
        Announcement ann = announcementMapper.selectById(id);
        if (ann == null || ann.getStatus() == null || ann.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在或未发布");
        }
        AnnouncementDetailDTO dto = new AnnouncementDetailDTO();
        BeanUtils.copyProperties(ann, dto);
        announcementMapper.incrementViewCount(id);
        return Result.ok(dto);
    }
}
