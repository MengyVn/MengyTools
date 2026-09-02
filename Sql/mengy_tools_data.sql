-- =====================================================================
-- MengyTools - 初始数据 (Seed)
-- 前置：先执行 mengy_tools_schema.sql
-- 执行： mysql -uroot -p mengy_tools < mengy_tools_data.sql
-- 默认管理员： admin / admin123  (登录后请立即修改密码！)
-- =====================================================================
USE `mengy_tools`;
SET NAMES utf8mb4;

-- -------------------------------------------------------------------
-- 角色
-- -------------------------------------------------------------------
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `sort`, `status`, `remark`) VALUES
  (1, '超级管理员', 'admin',  1, 1, '拥有所有权限'),
  (2, '普通编辑',   'editor', 2, 1, '仅拥有文章增删改查权限');

-- -------------------------------------------------------------------
-- 菜单 / 权限
-- menu_type: M=目录 C=菜单 F=按钮/权限
-- -------------------------------------------------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`) VALUES
-- 内容管理
(100, 0,   '内容管理', 'M', '',            '/content',     '',            'Document', 1, 1, 1),
(110, 100, '文章管理', 'C', '',            '/content/blog','',            'Edit',     1, 1, 1),
(111, 110, '文章列表', 'F', 'article:list',   '', '', '', 1, 1, 1),
(112, 110, '文章查询', 'F', 'article:query',  '', '', '', 2, 1, 1),
(113, 110, '文章新增', 'F', 'article:add',     '', '', '', 3, 1, 1),
(114, 110, '文章编辑', 'F', 'article:edit',    '', '', '', 4, 1, 1),
(115, 110, '文章删除', 'F', 'article:delete',  '', '', '', 5, 1, 1),
(116, 110, '文章发布', 'F', 'article:publish', '', '', '', 6, 1, 1),
(120, 100, '导航管理', 'C', '',            '/content/nav', '',           'Link',     2, 1, 1),
(121, 120, '导航列表', 'F', 'nav:list',   '', '', '', 1, 1, 1),
(122, 120, '导航新增', 'F', 'nav:add',    '', '', '', 2, 1, 1),
(123, 120, '导航编辑', 'F', 'nav:edit',   '', '', '', 3, 1, 1),
(124, 120, '导航删除', 'F', 'nav:delete', '', '', '', 4, 1, 1),
(130, 100, '工具管理', 'C', '',            '/content/tool', '',          'Tools',    3, 1, 1),
(131, 130, '工具列表', 'F', 'tool:list',   '', '', '', 1, 1, 1),
(132, 130, '工具新增', 'F', 'tool:add',    '', '', '', 2, 1, 1),
(133, 130, '工具编辑', 'F', 'tool:edit',   '', '', '', 3, 1, 1),
(134, 130, '工具删除', 'F', 'tool:delete', '', '', '', 4, 1, 1),
-- 系统管理
(200, 0,   '系统管理', 'M', '',            '/system',      '',            'Setting',  2, 1, 1),
(210, 200, '用户管理', 'C', '',            '/system/user', '',            'User',     1, 1, 1),
(211, 210, '用户列表', 'F', 'user:list',   '', '', '', 1, 1, 1),
(212, 210, '用户新增', 'F', 'user:add',    '', '', '', 2, 1, 1),
(213, 210, '用户编辑', 'F', 'user:edit',   '', '', '', 3, 1, 1),
(214, 210, '用户删除', 'F', 'user:delete', '', '', '', 4, 1, 1),
(220, 200, '角色管理', 'C', '',            '/system/role', '',            'UserFilled',2, 1, 1),
(221, 220, '角色列表', 'F', 'role:list',   '', '', '', 1, 1, 1),
(222, 220, '角色新增', 'F', 'role:add',    '', '', '', 2, 1, 1),
(223, 220, '角色编辑', 'F', 'role:edit',   '', '', '', 3, 1, 1),
(224, 220, '角色删除', 'F', 'role:delete', '', '', '', 4, 1, 1),
(230, 200, '菜单管理', 'C', '',            '/system/menu', '',            'Menu',     3, 1, 1),
(231, 230, '菜单列表', 'F', 'menu:list',   '', '', '', 1, 1, 1),
(232, 230, '菜单新增', 'F', 'menu:add',    '', '', '', 2, 1, 1),
(233, 230, '菜单编辑', 'F', 'menu:edit',   '', '', '', 3, 1, 1),
(234, 230, '菜单删除', 'F', 'menu:delete', '', '', '', 4, 1, 1);

-- -------------------------------------------------------------------
-- 用户 (admin / admin123)
-- 密码哈希由 Spring Security BCryptPasswordEncoder 生成，明文 admin123
-- -------------------------------------------------------------------
INSERT INTO `sys_user` (`id`, `username`, `nickname`, `password`, `status`, `remark`, `create_by`, `update_by`) VALUES
  (1, 'admin', '超级管理员',
   '$2a$10$XdtwtjKBuU9ygNv2r1Kgp.kUcMM9kldZ8HGBiSXBDQu0vtJwoYhzW',
   1, '默认超管，登录后请立即修改密码', 'system', 'system');

-- -------------------------------------------------------------------
-- 用户-角色： admin -> 超级管理员
-- -------------------------------------------------------------------
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- -------------------------------------------------------------------
-- 角色-菜单/权限授权
--   admin： 全部菜单/权限
--   editor： 仅 article:* (对应 need.md「普通编辑仅文章增删改查」)
-- -------------------------------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu`;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, `id` FROM `sys_menu` WHERE `permission` LIKE 'article:%';

-- -------------------------------------------------------------------
-- 演示内容（可删）
-- -------------------------------------------------------------------
INSERT INTO `blog_category` (`id`, `name`, `slug`, `sort`) VALUES
  (1, '技术笔记', 'tech', 1),
  (2, '生活随笔', 'life', 2);

INSERT INTO `nav_category` (`id`, `parent_id`, `name`, `icon`, `sort`) VALUES
  (1, 0, '常用工具', '', 1),
  (2, 0, '开发文档', '', 2);

INSERT INTO `nav_site` (`id`, `category_id`, `name`, `url`, `description`, `icon`, `sort`) VALUES
  (1, 1, 'GitHub',   'https://github.com',     '代码托管平台',   '', 1),
  (2, 1, 'JSON 解析', 'https://www.json.cn',   '在线 JSON 解析', '', 2),
  (3, 2, 'Spring',   'https://spring.io',      'Spring 官方',     '', 1),
  (4, 2, 'MDN',      'https://developer.mozilla.org', 'Web 开发文档', '', 2);
