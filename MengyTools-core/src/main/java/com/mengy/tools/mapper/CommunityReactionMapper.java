package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengy.tools.entity.CommunityReaction;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 社区点赞/收藏 Mapper。
 * 唯一索引 (user_id, target_type, target_id, type) 保证幂等：
 * INSERT IGNORE 返回 1=新增、0=已存在，据此决定是否调整冗余计数。
 */
@Mapper
public interface CommunityReactionMapper extends BaseMapper<CommunityReaction> {

    @Insert("""
        INSERT IGNORE INTO community_reaction (user_id, target_type, target_id, type, create_time)
        VALUES (#{userId}, #{targetType}, #{targetId}, #{type}, NOW())
        """)
    int insertIgnore(@Param("userId") Long userId,
                     @Param("targetType") String targetType,
                     @Param("targetId") Long targetId,
                     @Param("type") String type);

    @Delete("""
        DELETE FROM community_reaction
        WHERE user_id = #{userId} AND target_type = #{targetType}
          AND target_id = #{targetId} AND type = #{type}
        """)
    int deleteOne(@Param("userId") Long userId,
                  @Param("targetType") String targetType,
                  @Param("targetId") Long targetId,
                  @Param("type") String type);

    @Select("""
        SELECT COUNT(*) FROM community_reaction
        WHERE user_id = #{userId} AND target_type = #{targetType}
          AND target_id = #{targetId} AND type = #{type}
        """)
    int existsOne(@Param("userId") Long userId,
                  @Param("targetType") String targetType,
                  @Param("targetId") Long targetId,
                  @Param("type") String type);

    /** 某用户对一批对象的互动状态（列表页标红心用；P3 前端按需调用） */
    @Select("""
        <script>
        SELECT target_id FROM community_reaction
        WHERE user_id = #{userId} AND target_type = #{targetType} AND type = #{type}
          AND target_id IN
        <foreach collection="targetIds" item="tid" open="(" separator="," close=")">#{tid}</foreach>
        </script>
        """)
    java.util.List<Long> selectReactedIds(@Param("userId") Long userId,
                                          @Param("targetType") String targetType,
                                          @Param("type") String type,
                                          @Param("targetIds") java.util.List<Long> targetIds);
}
