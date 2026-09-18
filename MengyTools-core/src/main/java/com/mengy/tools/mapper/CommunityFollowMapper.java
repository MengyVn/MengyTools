package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.dto.CommunityFollowUserDTO;
import com.mengy.tools.entity.CommunityFollow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 社区关注 Mapper（关注用户）。
 * 与点赞同理：唯一索引 + INSERT IGNORE 保证幂等。
 * 标签关注维度已随标签功能一并移除，target_type 目前只有 user。
 */
@Mapper
public interface CommunityFollowMapper extends BaseMapper<CommunityFollow> {

    @Insert("""
        INSERT IGNORE INTO community_follow (user_id, target_type, target_id, create_time)
        VALUES (#{userId}, #{targetType}, #{targetId}, NOW())
        """)
    int insertIgnore(@Param("userId") Long userId,
                     @Param("targetType") String targetType,
                     @Param("targetId") Long targetId);

    @Delete("""
        DELETE FROM community_follow
        WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}
        """)
    int deleteOne(@Param("userId") Long userId,
                  @Param("targetType") String targetType,
                  @Param("targetId") Long targetId);

    @Select("""
        SELECT COUNT(*) FROM community_follow
        WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}
        """)
    int existsOne(@Param("userId") Long userId,
                  @Param("targetType") String targetType,
                  @Param("targetId") Long targetId);

    /**
     * 某用户的关注总数（主页展示用）。
     * 过滤掉已注销/被封禁的目标用户，保证「数字」与「关注列表条数」一致。
     */
    @Select("""
        SELECT COUNT(*) FROM community_follow f
        JOIN sys_user u ON u.id = f.target_id AND u.deleted = 0 AND u.status = 1
        WHERE f.user_id = #{userId} AND f.target_type = #{targetType}
        """)
    long countByUser(@Param("userId") Long userId, @Param("targetType") String targetType);

    /** 某用户被关注数（粉丝数）；同样过滤掉已注销/被封禁的粉丝，与粉丝列表口径一致 */
    @Select("""
        SELECT COUNT(*) FROM community_follow f
        JOIN sys_user u ON u.id = f.user_id AND u.deleted = 0 AND u.status = 1
        WHERE f.target_type = #{targetType} AND f.target_id = #{targetId}
        """)
    long countByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    /**
     * TA 关注的人（分页）。
     * followed=当前访问者是否已关注该行用户（未登录传 viewerId=0，恒为 false）。
     */
    @Select("""
        SELECT u.id, u.nickname, u.avatar, u.signature,
               f.create_time AS follow_time,
               EXISTS(SELECT 1 FROM community_follow v
                       WHERE v.user_id = #{viewerId} AND v.target_type = 'user' AND v.target_id = u.id) AS followed
        FROM community_follow f
        JOIN sys_user u ON u.id = f.target_id AND u.deleted = 0 AND u.status = 1
        WHERE f.user_id = #{userId} AND f.target_type = 'user'
        ORDER BY f.create_time DESC, f.id DESC
        """)
    IPage<CommunityFollowUserDTO> selectFollowingPage(IPage<CommunityFollowUserDTO> page,
                                                     @Param("userId") Long userId,
                                                     @Param("viewerId") Long viewerId);

    /** 关注 TA 的人（粉丝列表，分页） */
    @Select("""
        SELECT u.id, u.nickname, u.avatar, u.signature,
               f.create_time AS follow_time,
               EXISTS(SELECT 1 FROM community_follow v
                       WHERE v.user_id = #{viewerId} AND v.target_type = 'user' AND v.target_id = u.id) AS followed
        FROM community_follow f
        JOIN sys_user u ON u.id = f.user_id AND u.deleted = 0 AND u.status = 1
        WHERE f.target_type = 'user' AND f.target_id = #{userId}
        ORDER BY f.create_time DESC, f.id DESC
        """)
    IPage<CommunityFollowUserDTO> selectFollowerPage(IPage<CommunityFollowUserDTO> page,
                                                     @Param("userId") Long userId,
                                                     @Param("viewerId") Long viewerId);

    /**
     * 粉丝 ID 列表（发帖后推送通知用）。
     * 只取有效账号，避免给已注销/被封禁用户堆通知。
     */
    @Select("""
        SELECT f.user_id FROM community_follow f
        JOIN sys_user u ON u.id = f.user_id AND u.deleted = 0 AND u.status = 1
        WHERE f.target_type = 'user' AND f.target_id = #{userId}
        ORDER BY f.id DESC
        LIMIT #{limit}
        """)
    List<Long> selectFollowerIds(@Param("userId") Long userId, @Param("limit") int limit);
}
