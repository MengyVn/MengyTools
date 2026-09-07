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
     * 当前生效公告（铃铛下拉用，status=1 且已到发布时间，不再按 expire_time 过滤）。
     * 跑马灯优先、常驻优先、发布时间倒序。
     */
    @Select("""
        SELECT id, title, content_format, is_persistent, is_marquee, display_duration,
               publish_time, expire_time, status, view_count, create_time
        FROM announcement
        WHERE deleted = 0 AND status = 1
          AND (publish_time IS NULL OR publish_time <= NOW())
        ORDER BY is_marquee DESC, is_persistent DESC, publish_time DESC
        """)
    List<AnnouncementListItemDTO> selectActiveList();

    /**
     * 跑马灯公告（is_marquee=1 且 status=1 且已到发布时间）。
     */
    @Select("""
        SELECT id, title, content_format, is_persistent, is_marquee, display_duration,
               publish_time, expire_time, status, view_count, create_time
        FROM announcement
        WHERE deleted = 0 AND status = 1 AND is_marquee = 1
          AND (publish_time IS NULL OR publish_time <= NOW())
        ORDER BY is_persistent DESC, publish_time DESC
        """)
    List<AnnouncementListItemDTO> selectMarqueeList();

    /**
     * 历史公告分页（status=1 且已到发布时间，按 publish_time 倒序，不含 content）。
     */
    @Select("""
        SELECT id, title, content_format, is_persistent, is_marquee, display_duration,
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
