-- =====================================================================
-- MengyTools - 公告跑马灯改造（增量脚本）
-- 新增字段：
--   is_marquee        是否滚动出现在首页顶部跑马灯(1=是 0=否)
--   display_duration  跑马灯显示时长(分钟)，0/null=一直显示直到用户手动关闭
-- 原字段语义变更：
--   expire_time       不再用于过滤（保留字段，向后兼容）
--   is_persistent     1=常驻(每次登录/清缓存/换设备都重新弹出，关闭态存sessionStorage)
--                     0=非常驻(用户手动关闭后跨登录保持关闭，关闭态存localStorage)
-- 执行： mysql -uroot -p mengy_tools < mengy_tools_announcement_marquee.sql
-- =====================================================================
USE `mengy_tools`;
SET NAMES utf8mb4;

-- 增量添加字段（IF NOT EXISTS 语义用 information_schema 判断）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'mengy_tools' AND TABLE_NAME = 'announcement'
      AND COLUMN_NAME = 'is_marquee');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `announcement` ADD COLUMN `is_marquee` tinyint NOT NULL DEFAULT 0 COMMENT ''是否滚动出现在首页顶部跑马灯：1是 0否'' AFTER `is_persistent`',
    'SELECT ''is_marquee already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'mengy_tools' AND TABLE_NAME = 'announcement'
      AND COLUMN_NAME = 'display_duration');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `announcement` ADD COLUMN `display_duration` int NOT NULL DEFAULT 0 COMMENT ''跑马灯显示时长(分钟)：0/null=一直显示直到手动关闭'' AFTER `is_marquee`',
    'SELECT ''display_duration already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
