-- =====================================================================
-- V17：Excel 导入导出 / 序列号追溯 / 订金管理
-- =====================================================================

-- 序列号台账
CREATE TABLE IF NOT EXISTS erp_serial_track (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  serial_no     VARCHAR(64)  NOT NULL COMMENT '序列号',
  product_id    BIGINT       NOT NULL,
  warehouse_id  BIGINT       NOT NULL DEFAULT 0,
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 在库 2 已出库',
  inbound_no    VARCHAR(32)           DEFAULT '' COMMENT '入库单号',
  inbound_time  DATETIME              NULL,
  outbound_no   VARCHAR(32)           DEFAULT '' COMMENT '出库单号',
  outbound_time DATETIME              NULL,
  batch_no      VARCHAR(64)           DEFAULT '',
  creator       VARCHAR(64)           DEFAULT '',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_serial_product (serial_no, product_id),
  KEY idx_product_status (product_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '序列号追溯';

-- 订金字段
ALTER TABLE erp_sale_order     ADD COLUMN deposit_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已收订金' AFTER remark;
ALTER TABLE erp_purchase_order ADD COLUMN deposit_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已付订金' AFTER remark;

-- 菜单：更多功能 → 序列号追溯
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15230, '序列号追溯', '', 2, 5, 15020, 'serial-trace', 'more/serial-trace/index'),
  (15231, '序列号查询', 'stk:serial:query', 3, 1, 15230, '', '');
