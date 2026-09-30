-- =====================================================================
-- 登录日志 + IP 封禁（安全审计）
--
-- 用途：
--   1. sys_login_log  记录每一次登录尝试（成功/失败），含 IP 归属地、浏览器、时间；
--   2. sys_ip_ban     持久化封禁名单（区别于 Redis 的「5 次失败锁 30 分钟」临时锁定）；
--   3. sys_menu       新增「安全审计」目录与子菜单，并授权给超级管理员角色。
--
-- 执行方式（在项目库上执行一次，可重复执行）：
--   mysql -uroot -p mengy_tools < Sql/mengy_tools_security.sql
--
-- 注意：菜单 700 段为本次新增，不与既有 100/200/300/400/500/600 段冲突。
-- =====================================================================

-- ---------------------------------------------------------------- 登录日志
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id`         bigint       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `username`   varchar(64)  NOT NULL DEFAULT '' COMMENT '登录名（账号不存在时为用户输入值）',
  `user_id`    bigint       NULL     DEFAULT NULL COMMENT '用户ID（登录成功时回填）',
  `ip`         varchar(64)  NOT NULL DEFAULT '' COMMENT '来源IP',
  `region`     varchar(128) NOT NULL DEFAULT '' COMMENT 'IP归属地（离线库解析，内网/本机单独标记）',
  `user_agent` varchar(512) NOT NULL DEFAULT '' COMMENT '浏览器 UA',
  `browser`    varchar(64)  NOT NULL DEFAULT '' COMMENT '浏览器',
  `os`         varchar(64)  NOT NULL DEFAULT '' COMMENT '操作系统',
  `status`     tinyint      NOT NULL DEFAULT '1' COMMENT '结果:1成功 0失败',
  `message`    varchar(255) NOT NULL DEFAULT '' COMMENT '结果说明（失败原因 / 成功提示）',
  `login_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_ip` (`ip`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志（含 IP 归属地）';

-- ---------------------------------------------------------------- IP 封禁
-- 一个 IP 一行：重复封禁走 UPDATE（uk_ip 唯一），解除封禁置 status=0，
-- 保留历史（谁封的、什么时候、为什么），再次封禁直接复用该行。
CREATE TABLE IF NOT EXISTS `sys_ip_ban` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ip`            varchar(64)  NOT NULL COMMENT '被封禁的IP',
  `region`        varchar(128) NOT NULL DEFAULT '' COMMENT 'IP归属地',
  `reason`        varchar(255) NOT NULL DEFAULT '' COMMENT '封禁原因',
  `operator_id`   bigint       NULL     DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64)  NOT NULL DEFAULT '' COMMENT '操作人',
  `expire_time`   datetime     NULL     DEFAULT NULL COMMENT '解封时间(NULL=永久封禁)',
  `status`        tinyint      NOT NULL DEFAULT '1' COMMENT '状态:1封禁中 0已解除',
  `ban_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '封禁时间',
  `release_time`  datetime     NULL     DEFAULT NULL COMMENT '解除时间',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ip` (`ip`),
  KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IP封禁名单';

-- ---------------------------------------------------------------- 菜单
-- 700 安全审计（目录） → 710 登录日志 / 720 IP 封禁
INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `deleted`)
VALUES
  (700, 0,   '安全审计', 'M', '',                        'security',           '',                      'Lock',        7, 1, 1, 0),
  (710, 700, '登录日志', 'C', '',                        'security/login-log', 'security/login-log',    'Tickets',     1, 1, 1, 0),
  (711, 710, '日志列表', 'F', 'security:loginlog:list',  '',                   '',                      '',            1, 1, 1, 0),
  (712, 710, '日志清理', 'F', 'security:loginlog:clear', '',                   '',                      '',            2, 1, 1, 0),
  (720, 700, 'IP 封禁',  'C', '',                        'security/ip-ban',    'security/ip-ban',       'CircleClose', 2, 1, 1, 0),
  (721, 720, '封禁名单', 'F', 'security:ipban:list',     '',                   '',                      '',            1, 1, 1, 0),
  (722, 720, '封禁IP',   'F', 'security:ipban:ban',      '',                   '',                      '',            2, 1, 1, 0),
  (723, 720, '解除封禁', 'F', 'security:ipban:unban',    '',                   '',                      '',            3, 1, 1, 0);

-- ---------------------------------------------------------------- 角色授权
-- 超级管理员（role_key=admin，走 * 通配）天然可见；
-- 这里额外授给「超级管理员」自定义角色（id=3，role_key=Mengy），
-- 否则该角色只能看到菜单表里已授权的 40 项，看不到本次新增的安全审计。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (1, 700), (1, 710), (1, 711), (1, 712), (1, 720), (1, 721), (1, 722), (1, 723),
  (3, 700), (3, 710), (3, 711), (3, 712), (3, 720), (3, 721), (3, 722), (3, 723);
