-- =====================================================================
-- V14：功能丰富批次 —— 经营分析报表 / CSV 数据导入 / 库存预警扫描
-- 后端新增：销售排行、采购排行、库存 ABC、客户对账单、CSV 导入（商品/往来/期初）、预警手动扫描
-- =====================================================================

-- 报表中心（15010）下新增四个分析报表
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15199, '销售排行', '', 2, 10, 15010, 'rank-sale', 'report/rank-sale/index'),
  (15200, '排行查看', 'rpt:rank:query', 3, 1, 15199, '', ''),
  (15201, '采购排行', '', 2, 11, 15010, 'rank-purchase', 'report/rank-purchase/index'),
  (15202, '排行查看', 'rpt:rank:query', 3, 1, 15201, '', ''),
  (15203, '库存 ABC 分析', '', 2, 12, 15010, 'stock-abc', 'report/stock-abc/index'),
  (15204, 'ABC 查看', 'rpt:abc:query', 3, 1, 15203, '', ''),
  (15205, '客户对账单', '', 2, 13, 15010, 'customer-statement', 'report/customer-statement/index'),
  (15206, '对账单查看', 'rpt:statement:query', 3, 1, 15205, '', '');

-- 数据导入按钮权限（挂系统管理目录 1 下）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15207, '数据导入', 'infra:import:create', 3, 99, 1, '', '');

-- 预警扫描按钮权限（挂库存管理目录 4 下，复用 quant 查询权限也可，此处显式化）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15208, '预警扫描', 'stk:quant:query', 3, 99, 4, '', '');
