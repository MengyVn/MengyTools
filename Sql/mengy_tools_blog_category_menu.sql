-- =====================================================================
-- MengyTools - 博客分类管理菜单补丁
-- 在已有 sys_menu 数据基础上补「博客分类管理」菜单与权限
-- 仅 DDL/DML 增量，不重建表，不影响已有数据
-- 执行： mysql -uroot -p mengy_tools < mengy_tools_blog_category_menu.sql
-- =====================================================================
USE `mengy_tools`;
SET NAMES utf8mb4;

-- 1) 插入「博客分类管理」菜单（父级 100=内容管理，与 data.sql 中 110 文章管理同级）
--    使用 id 130~135，避免与现有 110-116/120-124 冲突
INSERT INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `deleted`)
VALUES
  (130, 100, '博客分类', 'C', '',              '/content/blog-category','', 'Files', 2, 1, 1, 0),
  (131, 130, '分类列表', 'F', 'blog:category:list',   '', '', '', 1, 1, 1, 0),
  (132, 130, '分类新增', 'F', 'blog:category:add',    '', '', '', 2, 1, 1, 0),
  (133, 130, '分类编辑', 'F', 'blog:category:edit',   '', '', '', 3, 1, 1, 0),
  (134, 130, '分类删除', 'F', 'blog:category:delete', '', '', '', 4, 1, 1, 0);

-- 2) 把新增的分类管理权限分配给：
--    - admin 角色（id=1，全量权限）
--    - editor 角色（id=2，need.md 约定仅文章+分类相关）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
  FROM `sys_role` r
  JOIN `sys_menu` m ON m.id BETWEEN 130 AND 134
  WHERE r.role_key IN ('admin', 'editor')
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

-- 3) 校验：查询 admin/editor 拥有的博客相关权限
-- SELECT r.role_key, m.menu_name, m.permission
--   FROM sys_role r
--   JOIN sys_role_menu rm ON rm.role_id = r.id
--   JOIN sys_menu m ON m.id = rm.menu_id
--  WHERE r.role_key IN ('admin','editor') AND (m.permission LIKE 'article:%' OR m.permission LIKE 'blog:category:%')
--  ORDER BY r.role_key, m.id;
