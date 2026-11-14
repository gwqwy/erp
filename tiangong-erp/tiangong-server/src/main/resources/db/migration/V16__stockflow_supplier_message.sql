-- =====================================================================
-- V16：收发存汇总 / 供应商对账单 / 消息中心 菜单
-- =====================================================================

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15220, '收发存汇总', '', 2, 14, 15010, 'stock-flow', 'report/stock-flow/index'),
  (15221, '收发存查询', 'rpt:stockflow:query', 3, 1, 15220, '', ''),
  (15222, '供应商对账单', '', 2, 15, 15010, 'supplier-statement', 'report/supplier-statement/index'),
  (15223, '对账单查看', 'rpt:statement:query', 3, 1, 15222, '', ''),
  (15224, '消息中心', '', 2, 21, 1, 'message', 'system/message/index');
