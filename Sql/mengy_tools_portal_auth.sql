-- =====================================================================
-- MengyTools - 门户登录/注册/个人中心 模块
-- 含 sys_user 字段扩展 + 普通用户角色
-- 增量脚本，不重建已有表，不影响已有数据
-- 执行： mysql -uroot -p mengy_tools < mengy_tools_portal_auth.sql
-- =====================================================================
USE `mengy_tools`;
SET NAMES utf8mb4;

-- -------------------------------------------------------------------
-- 1) sys_user 扩展字段
--    nickname_update_time : 昵称最后修改时间，用于「3日只能改一次」校验
--    register_ip           : 注册 IP，用于注册防刷溯源
-- -------------------------------------------------------------------
ALTER TABLE `sys_user`
  ADD COLUMN `nickname_update_time` datetime DEFAULT NULL COMMENT '昵称最后修改时间' AFTER `nickname`,
  ADD COLUMN `register_ip`           varchar(64) NOT NULL DEFAULT '' COMMENT '注册IP' AFTER `nickname_update_time`;

-- -------------------------------------------------------------------
-- 2) 普通用户角色（门户注册用户默认角色）
--    id=3，避免与 admin(1)/editor(2) 冲突；可重复执行
-- -------------------------------------------------------------------
INSERT IGNORE INTO `sys_role` (`id`, `role_name`, `role_key`, `sort`, `status`, `remark`)
VALUES (3, '普通用户', 'user', 3, 1, '门户注册用户，无后台权限');

-- 校验：SELECT id, role_name, role_key FROM sys_role ORDER BY id;
