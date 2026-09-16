package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站点配置实体，对应表 sys_setting（键值对）。
 * 主键为字符串键（如 site.default_landing），故使用 IdType.INPUT。
 */
@Data
@TableName("sys_setting")
public class SysSetting implements Serializable {

    @TableId(value = "setting_key", type = IdType.INPUT)
    private String settingKey;

    private String settingValue;

    private String remark;

    private String updateBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
