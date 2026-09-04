package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.dto.AnnouncementListItemDTO;
import com.mengy.tools.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 公告 Mapper。
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    /**
     * 当前生效公告（status=1 且未删除 且已到发布时间 且未消失）。
     * 常驻公告(is_persistent=1) 永不消失；非常驻公告需 expire_time > NOW()。
     * 常驻优先、发布时间倒序。
     */
    @Select("""
        SELECT id, title, content_format, is_persistent,
               publish_time, expire_time, status, view_count, create_time
        FROM announcement
        WHERE deleted = 0 AND status = 1
          AND (publish_time IS NULL OR publish_time <= NOW())
          AND (is_persistent = 1 OR (expire_time IS NOT NULL AND expire_time > NOW()))
        ORDER BY is_persistent DESC, publish_time DESC
        """)
    List<AnnouncementListItemDTO> selectActiveList();

    /**
     * 历史公告分页（status=1 且已到发布时间，按 publish_time 倒序，不含 content）。
     */
    @Select("""
        SELECT id, title, content_format, is_persistent,
               publish_time, expire_time, status, view_count, create_time
        FROM announcement
        WHERE deleted = 0 AND status = 1
          AND (publish_time IS NULL OR publish_time <= NOW())
        ORDER BY publish_time DESC
        """)
    IPage<AnnouncementListItemDTO> selectPortalPage(IPage<AnnouncementListItemDTO> page);

    /**
     * 阅读量自增（原子操作，避免读改写竞态）。
     */
    @Update("UPDATE announcement SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);
}
