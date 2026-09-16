package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengy.tools.entity.CommunityFollow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 社区关注 Mapper（关注用户或标签）。
 * 与点赞同理：唯一索引 + INSERT IGNORE 保证幂等。
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

    /** 某用户的关注总数（主页展示用） */
    @Select("SELECT COUNT(*) FROM community_follow WHERE user_id = #{userId} AND target_type = #{targetType}")
    long countByUser(@Param("userId") Long userId, @Param("targetType") String targetType);

    /** 某对象被关注数（用户/标签被关注数） */
    @Select("SELECT COUNT(*) FROM community_follow WHERE target_type = #{targetType} AND target_id = #{targetId}")
    long countByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);
}
