package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.entity.CommunityAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 社区治理留痕 Mapper（只增不改，无逻辑删除）。
 */
@Mapper
public interface CommunityAuditLogMapper extends BaseMapper<CommunityAuditLog> {

    /**
     * 操作日志分页（可按动作/对象类型/对象ID过滤）。
     */
    @Select("""
        <script>
        SELECT id, operator_id, operator_name, action, target_type, target_id,
               before_value, after_value, note, ip, create_time
        FROM community_audit_log
        <where>
          <if test="action != null and action != ''"> AND action = #{action} </if>
          <if test="targetType != null and targetType != ''"> AND target_type = #{targetType} </if>
          <if test="targetId != null"> AND target_id = #{targetId} </if>
        </where>
        ORDER BY id DESC
        </script>
        """)
    IPage<CommunityAuditLog> selectAdminPage(IPage<CommunityAuditLog> page,
                                             @Param("action") String action,
                                             @Param("targetType") String targetType,
                                             @Param("targetId") Long targetId);
}
