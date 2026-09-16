package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区写入侧需要的用户状态（独立于 SysUser 实体）。
 *
 * 为什么不直接给 SysUser 加 muteUntil 字段：门户/管理端也在查 sys_user，
 * 若实体新增该字段，在「代码已更新但迁移未执行」的窗口期会报表不存在。
 * 这里用独立查询把社区功能与既有代码解耦。
 */
@Data
public class CommunityUserStateDTO implements Serializable {

    private Long id;

    /** 1启用 0禁用（封禁即置 0） */
    private Integer status;

    /** 禁言到期时间，NULL=未禁言 */
    private LocalDateTime muteUntil;

    /** 注册时间（用于「新用户先审」判定） */
    private LocalDateTime createTime;
}
