-- =====================================================================
-- MengyTools - 导航分类管理菜单补丁
-- 在已有 sys_menu 数据基础上补「导航分类管理」菜单与权限
-- 现有 data.sql 已含导航站点管理（id 120-124, nav:list/add/edit/delete）
-- 此处仅补分类管理权限，不重建表，不影响已有数据
-- 执行： mysql -uroot -p mengy_tools < mengy_tools_nav_category_menu.sql
-- =====================================================================
USE `mengy_tools`;
SET NAMES utf8mb4;

-- 1) 插入「导航分类管理」菜单（父级 100=内容管理，与 110 文章/120 导航站点同级）
--    使用 id 140~144，避免与现有 110-116/120-124/130-134 冲突
INSERT INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `deleted`)
VALUES
  (140, 100, '导航分类', 'C', '',                    '/content/nav-category','', 'FolderOpened', 3, 1, 1, 0),
  (141, 140, '分类列表', 'F', 'nav:category:list',   '', '', '', 1, 1, 1, 0),
  (142, 140, '分类新增', 'F', 'nav:category:add',    '', '', '', 2, 1, 1, 0),
  (143, 140, '分类编辑', 'F', 'nav:category:edit',   '', '', '', 3, 1, 1, 0),
  (144, 140, '分类删除', 'F', 'nav:category:delete', '', '', '', 4, 1, 1, 0);

-- 2) 把新增的分类管理权限分配给 admin 角色（全量）；editor 默认不包含导航权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
  FROM `sys_role` r
  JOIN `sys_menu` m ON m.id BETWEEN 140 AND 144
  WHERE r.role_key = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );
