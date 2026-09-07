package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统用户实体，对应表 sys_user。
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String nickname;

    /** 昵称最后修改时间，用于「3日只能改一次」校验（门户个人中心） */
    private LocalDateTime nicknameUpdateTime;

    /** 注册 IP（门户注册防刷溯源） */
    private String registerIp;

    /** 密码哈希（BCrypt），永不序列化到前端 */
    @JsonIgnore
    private String password;

    private String email;

    private String phone;

    private String avatar;

    /** 状态:0禁用 1启用 */
    private Integer status;

    private String loginIp;

    private LocalDateTime loginTime;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 非数据库字段：用户角色列表（用户列表接口填充） */
    @TableField(exist = false)
    private List<SysRole> roles;
}
