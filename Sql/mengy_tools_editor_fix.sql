-- =====================================================================
-- MengyTools - 修复 editor 角色菜单授权
-- 问题：原 mengy_tools_menu_refactor.sql 中 editor(role_id=2) 仅授权
--       permission LIKE 'article:%' 的 F 按钮（111-115），未授权
--       M（博客管理 100）/ C（文章管理 110 / 编辑文章 120）父级节点，
--       导致 editor 登录后 selectMenusByUserId 查不到任何 M/C 菜单，
--       侧边栏空，只看到首页。
-- 修复：补全博客管理 M + 文章管理 C + 编辑文章 C + 文章 5 个 F。
-- 执行： mysql -uroot -p mengy_tools < mengy_tools_editor_fix.sql
-- =====================================================================
USE `mengy_tools`;
SET NAMES utf8mb4;

-- 清空 editor 当前所有菜单授权（admin 不动）
DELETE FROM `sys_role_menu` WHERE `role_id` = 2;

-- 重建 editor 授权：完整菜单链路 M -> C -> F
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (2, 100),   -- 博客管理 M（一级目录，侧边栏可见）
  (2, 110),   -- 文章管理 C（菜单项，侧边栏可见）
  (2, 120),   -- 编辑文章 C（隐藏菜单，从文章列表跳转）
  (2, 111),   -- 文章列表 article:list
  (2, 112),   -- 文章查询 article:query
  (2, 113),   -- 文章新增 article:add
  (2, 114),   -- 文章编辑 article:edit
  (2, 115);   -- 文章删除 article:delete

-- 验证：
--   SELECT m.id, m.menu_name, m.menu_type, m.permission
--   FROM sys_role_menu rm JOIN sys_menu m ON rm.menu_id = m.id
--   WHERE rm.role_id = 2 ORDER BY m.id;
--   预期：100/110/120/111/112/113/114/115 共 8 条
