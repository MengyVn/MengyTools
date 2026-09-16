package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.entity.CommunityNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 社区通知 Mapper：写入用 BaseMapper，读取用下面几个定制查询。
 */
@Mapper
public interface CommunityNotificationMapper extends BaseMapper<CommunityNotification> {

    /** 未读数（顶栏红点） */
    @Select("SELECT COUNT(*) FROM community_notification WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    long countUnread(@Param("userId") Long userId);

    /** 全部标记已读 */
    @Update("UPDATE community_notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    int markAllRead(@Param("userId") Long userId);

    /** 通知分页（可按是否已读过滤） */
    @Select("""
        <script>
        SELECT id, user_id, type, actor_id, actor_name, actor_avatar, article_id, comment_id,
               title, content, is_read, create_time
        FROM community_notification
        WHERE deleted = 0 AND user_id = #{userId}
        <if test="unreadOnly"> AND is_read = 0 </if>
        ORDER BY id DESC
        </script>
        """)
    IPage<CommunityNotification> selectMyPage(IPage<CommunityNotification> page,
                                              @Param("userId") Long userId,
                                              @Param("unreadOnly") boolean unreadOnly);
}
