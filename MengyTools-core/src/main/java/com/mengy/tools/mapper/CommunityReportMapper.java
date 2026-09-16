package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.dto.CommunityReportDTO;
import com.mengy.tools.entity.CommunityReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 社区举报 Mapper（P1 只读，处理动作在 P3）。
 */
@Mapper
public interface CommunityReportMapper extends BaseMapper<CommunityReport> {

    /**
     * 后台举报列表（可按状态过滤），带被举报评论正文摘要。
     */
    @Select("""
        <script>
        SELECT r.id, r.reporter_id, r.reporter_name, r.target_type, r.target_id, r.reason, r.detail,
               r.status, r.handler_id, r.handler_name, r.handle_note, r.handle_time, r.create_time,
               (SELECT LEFT(c.content, 120) FROM community_comment c
                 WHERE c.id = r.target_id AND r.target_type = 'comment') AS target_excerpt
        FROM community_report r
        WHERE r.deleted = 0
        <if test="status != null"> AND r.status = #{status} </if>
        ORDER BY r.status ASC, r.id DESC
        </script>
        """)
    IPage<CommunityReportDTO> selectAdminPage(IPage<CommunityReportDTO> page,
                                              @Param("status") Integer status);

    /** 同一用户对同一目标在 24 小时内的举报次数（防重复举报刷屏） */
    @Select("""
        SELECT COUNT(*) FROM community_report
        WHERE reporter_id = #{reporterId} AND target_type = #{targetType} AND target_id = #{targetId}
          AND deleted = 0 AND create_time > DATE_SUB(NOW(), INTERVAL 24 HOUR)
        """)
    Long countRecent(@Param("reporterId") Long reporterId,
                     @Param("targetType") String targetType,
                     @Param("targetId") Long targetId);

    /** 待处理举报数（后台徽标）。 */
    @Select("SELECT COUNT(*) FROM community_report WHERE deleted = 0 AND status = 0")
    Long countPending();
}
