-- =====================================================================
-- V13：文件中心 / IM 消息 挂入「文件协作」目录
-- （V12 中二者是无子菜单的顶级菜单，与其他模块的 目录→菜单 结构不一致）
-- =====================================================================

-- 新增目录：文件协作（15180 已被 CSV导出 按钮占用，取空闲 id 15198）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15198, '文件协作', '', 1, 45, 0, 'collab', '');

-- 两个菜单挂到目录下（按钮 15191-94 仍挂 15190，15196-97 仍挂 15195，无需变动）
UPDATE system_menu SET parent_id = 15198, sort = 1 WHERE id = 15190;
UPDATE system_menu SET parent_id = 15198, sort = 2 WHERE id = 15195;
