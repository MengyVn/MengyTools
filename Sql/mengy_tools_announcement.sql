-- =====================================================================
-- MengyTools - 公告管理模块
-- 含 announcement 表 DDL + 公告管理菜单/权限 DML
-- 增量脚本，不重建已有表，不影响已有数据
-- 执行： mysql -uroot -p mengy_tools < mengy_tools_announcement.sql
-- =====================================================================
USE `mengy_tools`;
SET NAMES utf8mb4;

-- -------------------------------------------------------------------
-- 1) 公告表 DDL
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title`          varchar(200) NOT NULL COMMENT '公告标题',
  `content`        text         NOT NULL COMMENT '公告内容（Markdown/HTML）',
  `content_format` varchar(20)  NOT NULL DEFAULT 'markdown' COMMENT '内容格式：markdown/html',
  `is_persistent`  tinyint      NOT NULL DEFAULT 0 COMMENT '是否常驻：1常驻(永不消失) 0非常驻',
  `publish_time`   datetime     DEFAULT NULL COMMENT '定时发布时间，NULL=立即发布',
  `expire_time`    datetime     DEFAULT NULL COMMENT '消失时间，常驻时为NULL',
  `status`         tinyint      NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布 2定时中 3已下线',
  `view_count`     int          NOT NULL DEFAULT 0 COMMENT '阅读量',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`, `publish_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告';

-- -------------------------------------------------------------------
-- 2) 公告管理菜单与权限
--    menu_type: M=目录 C=菜单 F=按钮/权限
--    id 400~414，避免与现有 100-144/200-234 冲突
--    公告为顶级菜单(parent_id=0)，与内容管理(100)/系统管理(200)同级
--    先清理可能的历史残留，保证可重复执行
-- -------------------------------------------------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 400 AND 414;
DELETE FROM `sys_menu`       WHERE `id`       BETWEEN 400 AND 414;

INSERT INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `deleted`)
VALUES
  (400, 0,   '公告管理', 'M', '',              'announce',         '',                      'Bell', 4, 1, 1, 0),
  (410, 400, '公告列表', 'C', '',              'announce/list',     'announce/announce-list','Bell', 1, 1, 1, 0),
  (411, 410, '公告列表', 'F', 'announce:list',   '', '', '', 1, 1, 1, 0),
  (412, 410, '公告新增', 'F', 'announce:add',    '', '', '', 2, 1, 1, 0),
  (413, 410, '公告编辑', 'F', 'announce:edit',   '', '', '', 3, 1, 1, 0),
  (414, 410, '公告删除', 'F', 'announce:delete', '', '', '', 4, 1, 1, 0);

-- -------------------------------------------------------------------
-- 3) 角色授权：
--    admin(role_id=1) 全量公告权限；editor(role_id=2) 不给公告权限
-- -------------------------------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` BETWEEN 400 AND 414;

-- -------------------------------------------------------------------
-- 校验：查询 admin 拥有的公告相关权限
-- SELECT r.role_key, m.menu_name, m.permission
--   FROM sys_role r
--   JOIN sys_role_menu rm ON rm.role_id = r.id
--   JOIN sys_menu m ON m.id = rm.menu_id
--  WHERE r.role_key = 'admin' AND (m.permission LIKE 'announce:%' OR m.id = 400 OR m.id = 410)
--  ORDER BY m.id;
