-- =====================================================================
-- MengyTools 个人数字资产平台 - 数据库结构 (DDL)
-- 对应 need.md 第 4 节 ER 设计；引擎 InnoDB / 字符集 utf8mb4
-- 默认库名 mengy_tools，与 application-dev.yml 中 MYSQL_DB 默认值一致
-- 执行： mysql -uroot -p < mengy_tools_schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `mengy_tools`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `mengy_tools`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------------------------------------------------
-- 权限模块 (RBAC)  need.md 3.3
-- -------------------------------------------------------------------

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`    varchar(64)  NOT NULL COMMENT '用户名',
  `nickname`    varchar(64)  NOT NULL DEFAULT '' COMMENT '昵称',
  `password`    varchar(128) NOT NULL COMMENT '密码哈希(BCrypt)',
  `email`       varchar(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  `phone`       varchar(32)  NOT NULL DEFAULT '' COMMENT '手机号',
  `avatar`      varchar(255) NOT NULL DEFAULT '' COMMENT '头像URL',
  `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
  `login_ip`    varchar(64)  NOT NULL DEFAULT '' COMMENT '最后登录IP',
  `login_time`  datetime     DEFAULT NULL COMMENT '最后登录时间',
  `remark`      varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`   varchar(64)  NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by`   varchar(64)  NOT NULL DEFAULT '' COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name`   varchar(64)  NOT NULL COMMENT '角色名称',
  `role_key`    varchar(64)  NOT NULL COMMENT '角色标识(如 admin/editor)',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `remark`      varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '菜单/权限ID',
  `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父级ID(0=顶级)',
  `menu_name`   varchar(64)  NOT NULL COMMENT '名称',
  `menu_type`   char(1)      NOT NULL DEFAULT 'C' COMMENT '类型:M目录 C菜单 F按钮/权限',
  `permission`  varchar(128) NOT NULL DEFAULT '' COMMENT '权限标识(如 article:publish)',
  `path`        varchar(255) NOT NULL DEFAULT '' COMMENT '前端路由',
  `component`   varchar(255) NOT NULL DEFAULT '' COMMENT '前端组件',
  `icon`        varchar(64)  NOT NULL DEFAULT '' COMMENT '图标',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
  `visible`     tinyint      NOT NULL DEFAULT 1 COMMENT '是否可见:0隐藏 1显示',
  `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_permission` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单与权限表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单/权限ID',
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单/权限关联表';

-- -------------------------------------------------------------------
-- 内容管理模块 (CMS)  need.md 3.1
-- -------------------------------------------------------------------

DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`        varchar(64)  NOT NULL COMMENT '分类名称',
  `slug`        varchar(64)  NOT NULL COMMENT '分类别名(路由用)',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客分类表';

DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name`        varchar(64)  NOT NULL COMMENT '标签名称',
  `slug`        varchar(64)  NOT NULL COMMENT '标签别名',
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客标签表';

DROP TABLE IF EXISTS `blog_article`;
CREATE TABLE `blog_article` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title`        varchar(200) NOT NULL COMMENT '标题',
  `summary`      varchar(500) NOT NULL DEFAULT '' COMMENT '摘要',
  `content`      longtext     COMMENT 'Markdown 内容(原文存储，前端解析)',
  `content_html` longtext     COMMENT '渲染后 HTML(可选缓存)',
  `content_format` varchar(20) NOT NULL DEFAULT 'markdown' COMMENT '内容格式: markdown / html',
  `cover`        varchar(255) NOT NULL DEFAULT '' COMMENT '封面URL',
  `category_id`  bigint       DEFAULT NULL COMMENT '分类ID',
  `status`       tinyint      NOT NULL DEFAULT 0 COMMENT '状态:0草稿 1已发布 2定时发布',
  `is_top`       tinyint      NOT NULL DEFAULT 0 COMMENT '是否置顶:0否 1是',
  `view_count`   bigint       NOT NULL DEFAULT 0 COMMENT '阅读量',
  `publish_time` datetime     DEFAULT NULL COMMENT '发布/定时发布时间',
  `author_id`    bigint       DEFAULT NULL COMMENT '作者用户ID',
  `deleted`      tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status_publish` (`status`, `publish_time`),
  KEY `idx_author` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章表';

DROP TABLE IF EXISTS `blog_article_tag`;
CREATE TABLE `blog_article_tag` (
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id`     bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`article_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章-标签关联表';

-- -------------------------------------------------------------------
-- 导航模块  need.md 3.1
-- -------------------------------------------------------------------

DROP TABLE IF EXISTS `nav_category`;
CREATE TABLE `nav_category` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父分类ID(0=顶级，支持多级)',
  `name`        varchar(64)  NOT NULL COMMENT '分类名称',
  `icon`        varchar(255) NOT NULL DEFAULT '' COMMENT '图标',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序权重',
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导航分类表';

DROP TABLE IF EXISTS `nav_site`;
CREATE TABLE `nav_site` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '站点ID',
  `category_id` bigint       NOT NULL COMMENT '所属分类ID',
  `name`        varchar(128) NOT NULL COMMENT '站点名称',
  `url`         varchar(500) NOT NULL COMMENT '站点URL',
  `description` varchar(500) NOT NULL DEFAULT '' COMMENT '描述',
  `icon`        varchar(255) NOT NULL DEFAULT '' COMMENT '图标',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序权重',
  `click_count` bigint       NOT NULL DEFAULT 0 COMMENT '点击量',
  `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导航站点表';

-- -------------------------------------------------------------------
-- 工具模块  need.md 3.1 (工具卡片动态配置)
-- -------------------------------------------------------------------

DROP TABLE IF EXISTS `sys_tool`;
CREATE TABLE `sys_tool` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `name`            varchar(128) NOT NULL COMMENT '工具名称',
  `description`     varchar(500) NOT NULL DEFAULT '' COMMENT '描述',
  `link`            varchar(500) NOT NULL DEFAULT '' COMMENT '跳转链接(外链)',
  `component_route` varchar(255) NOT NULL DEFAULT '' COMMENT '前端组件路由(站内工具)',
  `icon`            varchar(255) NOT NULL DEFAULT '' COMMENT '图标',
  `sort`            int          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`          tinyint      NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
  `deleted`         tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具卡片配置表';

-- -------------------------------------------------------------------
-- 数据统计与埋点模块  need.md 3.2
-- 注：数据量大时建议按月分表(如 sys_statistics_202609)
-- -------------------------------------------------------------------

DROP TABLE IF EXISTS `sys_statistics`;
CREATE TABLE `sys_statistics` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `resource_type` varchar(32) NOT NULL COMMENT '资源类型:article/tool/nav',
  `resource_id`  bigint       NOT NULL COMMENT '资源ID',
  `event_type`   varchar(32)  NOT NULL DEFAULT 'view' COMMENT '事件类型:view/click',
  `access_ip`    varchar(64)  NOT NULL DEFAULT '' COMMENT '访问IP',
  `user_agent`   varchar(500) NOT NULL DEFAULT '' COMMENT 'User-Agent',
  `access_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_resource` (`resource_type`, `resource_id`),
  KEY `idx_access_time` (`access_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问统计表(按月分表)';

SET FOREIGN_KEY_CHECKS = 1;
