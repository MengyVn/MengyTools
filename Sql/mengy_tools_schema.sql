-- =====================================================================
-- MengyTools 建表脚本（仅结构，不含业务数据）
--
-- 用途：新机器克隆项目后，用它把空库初始化出来，否则所有查询都会报
--       "Table 'mengy_tools.xxx' doesn't exist" 或登录直接 500。
--
-- 用法：
--   mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS mengy_tools CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
--   mysql -uroot -p mengy_tools < Sql/mengy_tools_schema.sql
--   mysql -uroot -p mengy_tools < Sql/mengy_tools_security.sql   -- 安全审计（登录日志/IP封禁+菜单）
--
-- 说明：
--   1. 脚本可重复执行（CREATE TABLE IF NOT EXISTS）；
--   2. 只有表结构，没有账号/菜单/分类等基础数据 —— 若要与旧机器完全一致，
--      请在旧机器执行：mysqldump -uroot -p mengy_tools > mengy_tools.sql
--      再到新机器：mysql -uroot -p mengy_tools < mengy_tools.sql
-- =====================================================================


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告内容（Markdown/HTML）',
  `content_format` varchar(20) NOT NULL DEFAULT 'markdown' COMMENT '内容格式：markdown/html',
  `is_persistent` tinyint NOT NULL DEFAULT '0' COMMENT '是否常驻：1常驻(永不消失) 0非常驻',
  `is_marquee` tinyint NOT NULL DEFAULT '0' COMMENT '是否滚动出现在首页顶部跑马灯：1是 0否',
  `display_duration` int NOT NULL DEFAULT '0' COMMENT '跑马灯显示时长(分钟)：0/null=一直显示直到手动关闭',
  `publish_time` datetime DEFAULT NULL COMMENT '定时发布时间，NULL=立即发布',
  `expire_time` datetime DEFAULT NULL COMMENT '消失时间，常驻时为NULL',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0草稿 1已发布 2定时中 3已下线',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '阅读量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`,`publish_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `blog_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `content_type` varchar(16) NOT NULL DEFAULT 'blog' COMMENT '内容类型:blog=站主博客 community=社区帖子',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `summary` varchar(500) NOT NULL DEFAULT '' COMMENT '摘要',
  `content` longtext COMMENT 'Markdown 内容(原文存储，前端解析)',
  `content_html` longtext COMMENT '渲染后 HTML(可选缓存)',
  `content_format` varchar(20) NOT NULL DEFAULT 'markdown' COMMENT '内容格式：markdown / html',
  `cover` varchar(255) NOT NULL DEFAULT '' COMMENT '封面URL',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态:0草稿 1已发布 2定时发布',
  `is_top` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶:0否 1是',
  `view_count` bigint NOT NULL DEFAULT '0' COMMENT '阅读量',
  `allow_comment` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许评论:1允许 0关闭',
  `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论数(冗余计数)',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数(冗余计数)',
  `favorite_count` int NOT NULL DEFAULT '0' COMMENT '收藏数(冗余计数)',
  `publish_time` datetime DEFAULT NULL COMMENT '发布/定时发布时间',
  `author_id` bigint DEFAULT NULL COMMENT '作者用户ID',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status_publish` (`status`,`publish_time`),
  KEY `idx_author` (`author_id`),
  KEY `idx_status_comment` (`status`,`comment_count`),
  KEY `idx_type_status_publish` (`content_type`,`status`,`publish_time`),
  FULLTEXT KEY `ft_article_search` (`title`,`summary`,`content`) /*!50100 WITH PARSER `ngram` */ 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客文章表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `blog_article_tag` (
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`article_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章-标签关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `blog_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `slug` varchar(64) NOT NULL COMMENT '分类别名(路由用)',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客分类表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `blog_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(64) NOT NULL COMMENT '标签名称',
  `slug` varchar(64) NOT NULL COMMENT '标签别名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客标签表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `community_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) NOT NULL DEFAULT '' COMMENT '操作人昵称(冗余)',
  `action` varchar(64) NOT NULL COMMENT '动作:comment.audit/comment.delete/report.handle/user.mute/user.ban',
  `target_type` varchar(16) NOT NULL DEFAULT '' COMMENT '对象类型:comment/article/user/report',
  `target_id` bigint NOT NULL DEFAULT '0' COMMENT '对象ID',
  `before_value` varchar(500) NOT NULL DEFAULT '' COMMENT '变更前(状态/字段摘要)',
  `after_value` varchar(500) NOT NULL DEFAULT '' COMMENT '变更后(状态/字段摘要)',
  `note` varchar(255) NOT NULL DEFAULT '' COMMENT '备注(处理理由)',
  `ip` varchar(64) NOT NULL DEFAULT '' COMMENT '操作来源IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_operator_time` (`operator_id`,`create_time`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_action_time` (`action`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区治理操作留痕表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `community_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id` bigint NOT NULL COMMENT '所属文章ID(blog_article.id)',
  `root_id` bigint NOT NULL DEFAULT '0' COMMENT '顶层评论ID：顶层评论为0，子回复指向顶层评论',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '直接父评论ID：顶层评论为0',
  `floor` int NOT NULL DEFAULT '0' COMMENT '楼层号：仅顶层评论有效，从1开始',
  `reply_to_user_id` bigint DEFAULT NULL COMMENT '被回复用户ID(子回复用)',
  `reply_to_name` varchar(64) NOT NULL DEFAULT '' COMMENT '被回复用户昵称(冗余)',
  `author_id` bigint NOT NULL COMMENT '评论人ID(sys_user.id)',
  `author_name` varchar(64) NOT NULL DEFAULT '' COMMENT '评论人昵称(冗余，避免列表N+1)',
  `author_avatar` varchar(255) NOT NULL DEFAULT '' COMMENT '评论人头像(冗余)',
  `content` text NOT NULL COMMENT '评论正文(纯文本，服务端限长)',
  `content_html` text COMMENT '消毒后的展示HTML(@提及/链接/换行)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:0待审 1已发布 2已屏蔽',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数(冗余计数)',
  `ip` varchar(64) NOT NULL DEFAULT '' COMMENT '来源IP(风控与留痕)',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0未删 1已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_status_time` (`article_id`,`status`,`deleted`,`create_time`),
  KEY `idx_root_time` (`root_id`,`status`,`create_time`),
  KEY `idx_author_time` (`author_id`,`create_time`),
  KEY `idx_status_time` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区评论表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `community_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '关注发起人ID',
  `target_type` varchar(16) NOT NULL COMMENT '对象类型:user/tag',
  `target_id` bigint NOT NULL COMMENT '对象ID(userId 或 tagId)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow` (`user_id`,`target_type`,`target_id`),
  KEY `idx_target` (`target_type`,`target_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区关注表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `community_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收者ID',
  `type` varchar(16) NOT NULL COMMENT '类型:reply/mention/like/system',
  `actor_id` bigint NOT NULL DEFAULT '0' COMMENT '触发者ID(系统消息为0)',
  `actor_name` varchar(64) NOT NULL DEFAULT '' COMMENT '触发者昵称(冗余)',
  `actor_avatar` varchar(255) NOT NULL DEFAULT '' COMMENT '触发者头像(冗余)',
  `article_id` bigint NOT NULL DEFAULT '0' COMMENT '关联文章ID',
  `comment_id` bigint NOT NULL DEFAULT '0' COMMENT '关联评论ID',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '通知标题',
  `content` varchar(255) NOT NULL DEFAULT '' COMMENT '通知摘要',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读:0未读 1已读',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0未删 1已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read_time` (`user_id`,`is_read`,`deleted`,`create_time`),
  KEY `idx_article` (`article_id`),
  KEY `idx_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `community_reaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_type` varchar(16) NOT NULL COMMENT '对象类型:article/comment',
  `target_id` bigint NOT NULL COMMENT '对象ID',
  `type` varchar(16) NOT NULL COMMENT '互动类型:like/favorite',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target_type` (`user_id`,`target_type`,`target_id`,`type`),
  KEY `idx_target` (`target_type`,`target_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区点赞收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `community_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `reporter_id` bigint NOT NULL COMMENT '举报人ID',
  `reporter_name` varchar(64) NOT NULL DEFAULT '' COMMENT '举报人昵称(冗余)',
  `target_type` varchar(16) NOT NULL COMMENT '对象类型:comment/article/user',
  `target_id` bigint NOT NULL COMMENT '对象ID',
  `reason` varchar(64) NOT NULL DEFAULT '' COMMENT '举报原因(枚举文案)',
  `detail` varchar(500) NOT NULL DEFAULT '' COMMENT '补充说明',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态:0待处理 1举报成立 2已驳回',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(64) NOT NULL DEFAULT '' COMMENT '处理人昵称(冗余)',
  `handle_note` varchar(255) NOT NULL DEFAULT '' COMMENT '处理说明',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0未删 1已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`status`,`create_time`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_reporter` (`reporter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区举报表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `nav_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类ID(0=顶级，支持多级)',
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `icon` varchar(255) NOT NULL DEFAULT '' COMMENT '图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导航分类表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `nav_site` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '站点ID',
  `category_id` bigint NOT NULL COMMENT '所属分类ID',
  `name` varchar(128) NOT NULL COMMENT '站点名称',
  `url` varchar(500) NOT NULL COMMENT '站点URL',
  `description` varchar(500) NOT NULL DEFAULT '' COMMENT '描述',
  `icon` varchar(255) NOT NULL DEFAULT '' COMMENT '图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `click_count` bigint NOT NULL DEFAULT '0' COMMENT '点击量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:0禁用 1启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导航站点表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_ip_ban` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ip` varchar(64) NOT NULL COMMENT '被封禁的IP',
  `region` varchar(128) NOT NULL DEFAULT '' COMMENT 'IP归属地',
  `reason` varchar(255) NOT NULL DEFAULT '' COMMENT '封禁原因',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) NOT NULL DEFAULT '' COMMENT '操作人',
  `expire_time` datetime DEFAULT NULL COMMENT '解封时间(NULL=永久封禁)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1封禁中 0已解除',
  `ban_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '封禁时间',
  `release_time` datetime DEFAULT NULL COMMENT '解除时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ip` (`ip`),
  KEY `idx_status_expire` (`status`,`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IP封禁名单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `username` varchar(64) NOT NULL DEFAULT '' COMMENT '登录名（账号不存在时为用户输入值）',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（登录成功时回填）',
  `ip` varchar(64) NOT NULL DEFAULT '' COMMENT '来源IP',
  `region` varchar(128) NOT NULL DEFAULT '' COMMENT 'IP归属地（离线库解析，内网/本机单独标记）',
  `user_agent` varchar(512) NOT NULL DEFAULT '' COMMENT '浏览器 UA',
  `browser` varchar(64) NOT NULL DEFAULT '' COMMENT '浏览器',
  `os` varchar(64) NOT NULL DEFAULT '' COMMENT '操作系统',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '结果:1成功 0失败',
  `message` varchar(255) NOT NULL DEFAULT '' COMMENT '结果说明（失败原因 / 成功提示）',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_ip` (`ip`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志（含 IP 归属地）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单/权限ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级ID(0=顶级)',
  `menu_name` varchar(64) NOT NULL COMMENT '名称',
  `menu_type` char(1) NOT NULL DEFAULT 'C' COMMENT '类型:M目录 C菜单 F按钮/权限',
  `permission` varchar(128) NOT NULL DEFAULT '' COMMENT '权限标识(如 article:publish)',
  `path` varchar(255) NOT NULL DEFAULT '' COMMENT '前端路由',
  `component` varchar(255) NOT NULL DEFAULT '' COMMENT '前端组件',
  `icon` varchar(64) NOT NULL DEFAULT '' COMMENT '图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否可见:0隐藏 1显示',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:0禁用 1启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_permission` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单与权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称',
  `role_key` varchar(64) NOT NULL COMMENT '角色标识(如 admin/editor)',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:0禁用 1启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单/权限ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-菜单/权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_setting` (
  `setting_key` varchar(64) NOT NULL COMMENT '配置键（域.名称，如 site.default_landing）',
  `setting_value` varchar(500) NOT NULL DEFAULT '' COMMENT '配置值',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '说明',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT '最后更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站点配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_statistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `resource_type` varchar(32) NOT NULL COMMENT '资源类型:article/tool/nav',
  `resource_id` bigint NOT NULL COMMENT '资源ID',
  `event_type` varchar(32) NOT NULL DEFAULT 'view' COMMENT '事件类型:view/click',
  `access_ip` varchar(64) NOT NULL DEFAULT '' COMMENT '访问IP',
  `user_agent` varchar(500) NOT NULL DEFAULT '' COMMENT 'User-Agent',
  `access_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_resource` (`resource_type`,`resource_id`),
  KEY `idx_access_time` (`access_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='访问统计表(按月分表)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_tool` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `name` varchar(128) NOT NULL COMMENT '工具名称',
  `description` varchar(500) NOT NULL DEFAULT '' COMMENT '描述',
  `link` varchar(500) NOT NULL DEFAULT '' COMMENT '跳转链接(外链)',
  `component_route` varchar(255) NOT NULL DEFAULT '' COMMENT '前端组件路由(站内工具)',
  `icon` varchar(255) NOT NULL DEFAULT '' COMMENT '图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:0禁用 1启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具卡片配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `nickname` varchar(64) NOT NULL DEFAULT '' COMMENT '昵称',
  `nickname_update_time` datetime DEFAULT NULL COMMENT '昵称最后修改时间',
  `register_ip` varchar(64) NOT NULL DEFAULT '' COMMENT '注册IP',
  `password` varchar(128) NOT NULL COMMENT '密码哈希(BCrypt)',
  `email` varchar(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  `phone` varchar(32) NOT NULL DEFAULT '' COMMENT '手机号',
  `avatar` varchar(255) NOT NULL DEFAULT '' COMMENT '头像URL',
  `signature` varchar(255) NOT NULL DEFAULT '' COMMENT '个人签名(社区公开主页展示)',
  `mute_until` datetime DEFAULT NULL COMMENT '禁言到期时间:NULL=未禁言(封禁用 status=0)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:0禁用 1启用',
  `login_ip` varchar(64) NOT NULL DEFAULT '' COMMENT '最后登录IP',
  `login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0未删 1已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

