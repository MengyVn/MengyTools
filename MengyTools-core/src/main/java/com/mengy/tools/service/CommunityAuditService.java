package com.mengy.tools.service;

import com.mengy.tools.entity.CommunityAuditLog;
import com.mengy.tools.mapper.CommunityAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 社区治理操作留痕（评论 / 帖子 / 举报 / 用户处置通用）。
 *
 * 抽成独立服务的原因：评论治理与帖子治理都要写日志，早期把这段逻辑放在
 * CommunityWriteService 里导致帖子侧无法复用，只能复制一份。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityAuditService {

    private final CommunityAuditLogMapper auditLogMapper;

    /**
     * 写一条治理日志。
     *
     * @param targetType comment / post / article / user / report
     * @param before     变更前摘要（如 status=1）
     * @param after      变更后摘要（如 status=2）
     */
    public void write(Long operatorId, String operatorName, String action, String targetType,
                      Long targetId, String before, String after, String note, String ip) {
        CommunityAuditLog row = new CommunityAuditLog();
        row.setOperatorId(operatorId);
        row.setOperatorName(operatorName == null ? "" : operatorName);
        row.setAction(action);
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        row.setBeforeValue(abbreviate(before, 500));
        row.setAfterValue(abbreviate(after, 500));
        row.setNote(note == null ? "" : abbreviate(note, 255));
        row.setIp(ip == null ? "" : ip);
        auditLogMapper.insert(row);
    }

    /** 摘要压缩：折叠空白并截断 */
    public static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
