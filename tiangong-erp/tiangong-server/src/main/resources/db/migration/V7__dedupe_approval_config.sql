-- =====================================================================
-- V7：审批配置去重 + 唯一约束（biz_type, level）
-- =====================================================================

-- 1. 同 (biz_type, level) 重复配置保留 id 最小的一条
DELETE t1 FROM bpm_approval_config t1
JOIN bpm_approval_config t2
  ON t1.biz_type = t2.biz_type AND t1.level = t2.level AND t1.id > t2.id;

-- 2. 唯一约束防再犯（逻辑删除行会占键，先物理清理已删除行）
DELETE FROM bpm_approval_config WHERE deleted = 1;
ALTER TABLE bpm_approval_config ADD UNIQUE KEY uk_biz_level (biz_type, level);

-- 3. 补销售成本结转凭证模板（借 6401 主营业务成本 / 贷 1405 库存商品）
INSERT INTO erp_voucher_template (biz_type, event, subject_id, direction, summary, amount_source, enabled) VALUES
  ('sale_out_cost', 'audit', 7, 'debit',  '结转销售成本', 'TOTAL', 1),
  ('sale_out_cost', 'audit', 4, 'credit', '转出库存商品', 'TOTAL', 1);
