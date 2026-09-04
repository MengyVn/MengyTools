-- =====================================================================
-- MengyTools - 博客文章内容格式字段迁移
-- 给 blog_article 表新增 content_format 列，支持 markdown / html 两种格式
-- 执行： mysql -uroot -p mengy_tools < mengy_tools_article_format.sql
-- =====================================================================
USE `mengy_tools`;
SET NAMES utf8mb4;

ALTER TABLE `blog_article`
  ADD COLUMN `content_format` VARCHAR(20) NOT NULL DEFAULT 'markdown'
  COMMENT '内容格式：markdown / html'
  AFTER `content_html`;

-- 兼容旧数据：原有文章一律标记为 markdown
UPDATE `blog_article` SET `content_format` = 'markdown' WHERE `content_format` IS NULL OR `content_format` = '';
