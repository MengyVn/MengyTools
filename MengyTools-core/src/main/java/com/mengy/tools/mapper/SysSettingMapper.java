package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengy.tools.entity.SysSetting;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 站点配置 Mapper。
 */
@Mapper
public interface SysSettingMapper extends BaseMapper<SysSetting> {

    /** 键值对 upsert（存在则更新值与更新人） */
    @Insert("""
        INSERT INTO sys_setting (setting_key, setting_value, remark, update_by)
        VALUES (#{key}, #{value}, #{remark}, #{operator})
        ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), update_by = VALUES(update_by)
        """)
    int upsert(@Param("key") String key,
               @Param("value") String value,
               @Param("remark") String remark,
               @Param("operator") String operator);
}
