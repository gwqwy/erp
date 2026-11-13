-- =====================================================================
-- V10：业务丰富 —— 库存调拨 / 盘点 / 销售退货 / 采购退货 / 其他出入库 / 信用额度
-- =====================================================================

CREATE TABLE IF NOT EXISTS erp_stock_transfer (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  no            VARCHAR(32)  NOT NULL COMMENT 'TFyyyyMMxxxx',
  from_warehouse_id BIGINT   NOT NULL COMMENT '调出仓',
  to_warehouse_id   BIGINT   NOT NULL COMMENT '调入仓',
  transfer_date DATE         NOT NULL,
  total_qty     DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark        VARCHAR(500),
  status        TINYINT      NOT NULL DEFAULT 10,
  audit_user_id BIGINT,
  audit_time    DATETIME,
  creator       VARCHAR(64)  NOT NULL DEFAULT '',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater       VARCHAR(64)  NOT NULL DEFAULT '',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id     BIGINT       NOT NULL DEFAULT 1,
  company_id    BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '库存调拨单';

CREATE TABLE IF NOT EXISTS erp_stock_transfer_item (
  id         BIGINT        NOT NULL AUTO_INCREMENT,
  transfer_id BIGINT       NOT NULL,
  product_id BIGINT        NOT NULL,
  qty        DECIMAL(20,6) NOT NULL,
  remark     VARCHAR(255),
  creator    VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater    VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id  BIGINT        NOT NULL DEFAULT 1,
  company_id BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_transfer (transfer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '调拨明细';

CREATE TABLE IF NOT EXISTS erp_stock_check (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  no            VARCHAR(32)  NOT NULL COMMENT 'CKyyyyMMxxxx',
  warehouse_id  BIGINT       NOT NULL,
  check_date    DATE         NOT NULL,
  remark        VARCHAR(500),
  status        TINYINT      NOT NULL DEFAULT 10,
  audit_user_id BIGINT,
  audit_time    DATETIME,
  creator       VARCHAR(64)  NOT NULL DEFAULT '',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater       VARCHAR(64)  NOT NULL DEFAULT '',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id     BIGINT       NOT NULL DEFAULT 1,
  company_id    BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '库存盘点单';

CREATE TABLE IF NOT EXISTS erp_stock_check_item (
  id         BIGINT        NOT NULL AUTO_INCREMENT,
  check_id   BIGINT        NOT NULL,
  product_id BIGINT        NOT NULL,
  book_qty   DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '账面数量（创建时快照）',
  real_qty   DECIMAL(20,6) NOT NULL COMMENT '实盘数量',
  creator    VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater    VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id  BIGINT        NOT NULL DEFAULT 1,
  company_id BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_check (check_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '盘点明细';

CREATE TABLE IF NOT EXISTS erp_sale_return (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  no            VARCHAR(32)  NOT NULL COMMENT 'SRyyyyMMxxxx',
  customer_id   BIGINT       NOT NULL,
  warehouse_id  BIGINT       NOT NULL,
  order_id      BIGINT                COMMENT '来源销售订单，可空',
  return_date   DATE         NOT NULL,
  total_qty     DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_amount  DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark        VARCHAR(500),
  status        TINYINT      NOT NULL DEFAULT 10,
  audit_user_id BIGINT,
  audit_time    DATETIME,
  creator       VARCHAR(64)  NOT NULL DEFAULT '',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater       VARCHAR(64)  NOT NULL DEFAULT '',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id     BIGINT       NOT NULL DEFAULT 1,
  company_id    BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '销售退货单';

CREATE TABLE IF NOT EXISTS erp_sale_return_item (
  id         BIGINT        NOT NULL AUTO_INCREMENT,
  return_id  BIGINT        NOT NULL,
  product_id BIGINT        NOT NULL,
  qty        DECIMAL(20,6) NOT NULL,
  price      DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark     VARCHAR(255),
  creator    VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater    VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id  BIGINT        NOT NULL DEFAULT 1,
  company_id BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_return (return_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '销售退货明细';

CREATE TABLE IF NOT EXISTS erp_purchase_return (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  no            VARCHAR(32)  NOT NULL COMMENT 'PRyyyyMMxxxx',
  supplier_id   BIGINT       NOT NULL,
  warehouse_id  BIGINT       NOT NULL,
  order_id      BIGINT                COMMENT '来源采购订单，可空',
  return_date   DATE         NOT NULL,
  total_qty     DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_amount  DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark        VARCHAR(500),
  status        TINYINT      NOT NULL DEFAULT 10,
  audit_user_id BIGINT,
  audit_time    DATETIME,
  creator       VARCHAR(64)  NOT NULL DEFAULT '',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater       VARCHAR(64)  NOT NULL DEFAULT '',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id     BIGINT       NOT NULL DEFAULT 1,
  company_id    BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '采购退货单';

CREATE TABLE IF NOT EXISTS erp_purchase_return_item (
  id         BIGINT        NOT NULL AUTO_INCREMENT,
  return_id  BIGINT        NOT NULL,
  product_id BIGINT        NOT NULL,
  qty        DECIMAL(20,6) NOT NULL,
  price      DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark     VARCHAR(255),
  creator    VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater    VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id  BIGINT        NOT NULL DEFAULT 1,
  company_id BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_return (return_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '采购退货明细';

CREATE TABLE IF NOT EXISTS erp_stock_other (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  no            VARCHAR(32)  NOT NULL COMMENT 'QTyymmxxxx 入 / QT出 同号段',
  other_type    VARCHAR(16)  NOT NULL COMMENT 'other_in 其他入库 / other_out 其他出库',
  warehouse_id  BIGINT       NOT NULL,
  other_date    DATE         NOT NULL,
  total_qty     DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark        VARCHAR(500)          COMMENT '如赠品入库/报废/领用',
  status        TINYINT      NOT NULL DEFAULT 10,
  audit_user_id BIGINT,
  audit_time    DATETIME,
  creator       VARCHAR(64)  NOT NULL DEFAULT '',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater       VARCHAR(64)  NOT NULL DEFAULT '',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id     BIGINT       NOT NULL DEFAULT 1,
  company_id    BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '其他出入库';

CREATE TABLE IF NOT EXISTS erp_stock_other_item (
  id         BIGINT        NOT NULL AUTO_INCREMENT,
  other_id   BIGINT        NOT NULL,
  product_id BIGINT        NOT NULL,
  qty        DECIMAL(20,6) NOT NULL,
  price      DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '入库成本（出库时忽略）',
  remark     VARCHAR(255),
  creator    VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater    VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id  BIGINT        NOT NULL DEFAULT 1,
  company_id BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_other (other_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '其他出入库明细';

-- 费用登记（生成凭证：借 6601 管理费用 / 贷 1001 现金）
CREATE TABLE IF NOT EXISTS erp_finance_expense (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  no            VARCHAR(32)   NOT NULL COMMENT 'FYyyyyMMxxxx',
  expense_date  DATE          NOT NULL,
  category      VARCHAR(32)   NOT NULL COMMENT '办公/差旅/租金/水电/其他',
  amount        DECIMAL(20,6) NOT NULL,
  account_id    BIGINT        NOT NULL COMMENT '支付账户',
  remark        VARCHAR(500),
  status        TINYINT       NOT NULL DEFAULT 10,
  audit_user_id BIGINT,
  audit_time    DATETIME,
  creator       VARCHAR(64)   NOT NULL DEFAULT '',
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater       VARCHAR(64)   NOT NULL DEFAULT '',
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id     BIGINT        NOT NULL DEFAULT 1,
  company_id    BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '费用登记';

-- 操作日志（AOP 自动记录写操作）
CREATE TABLE IF NOT EXISTS system_oper_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT,
  username    VARCHAR(32),
  url         VARCHAR(255) NOT NULL,
  http_method VARCHAR(8)   NOT NULL,
  params_text VARCHAR(2000),
  cost_ms     BIGINT       NOT NULL DEFAULT 0,
  success     BIT(1)       NOT NULL DEFAULT b'1',
  error_msg   VARCHAR(500),
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '操作日志';

-- 公告通知
CREATE TABLE IF NOT EXISTS infra_notice (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  title        VARCHAR(128) NOT NULL,
  content      TEXT,
  publish_user VARCHAR(32),
  publish_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 发布 1 撤回',
  creator      VARCHAR(64)  NOT NULL DEFAULT '',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)  NOT NULL DEFAULT '',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id    BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '公告通知';

-- 费用凭证模板：借 6601 管理费用 / 贷 1001 库存现金
INSERT INTO erp_voucher_template (biz_type, event, subject_id, direction, summary, amount_source, enabled) VALUES
  ('finance_expense', 'audit', 9, 'debit',  '费用支出', 'TOTAL', 1),
  ('finance_expense', 'audit', 1, 'credit', '现金支付', 'TOTAL', 1);

-- 科目 9：6601 管理费用
INSERT INTO erp_subject (id, no, name, direction, status) VALUES (10, '6601', '管理费用', 1, 0);

-- ---------------------------------------------------------------------
-- 菜单（id 段 15120+）
-- ---------------------------------------------------------------------
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15120, '费用登记', '', 2, 7, 6, 'expense', 'fin/expense/index'),
  (15121, '费用查询', 'fin:expense:query', 3, 1, 15120, '', ''),
  (15122, '费用新增', 'fin:expense:create', 3, 2, 15120, '', ''),
  (15123, '费用删除', 'fin:expense:delete', 3, 3, 15120, '', ''),
  (15124, '费用审核', 'fin:expense:audit', 3, 4, 15120, '', ''),
  (15125, '应收账龄', '', 2, 8, 6, 'aging', 'fin/aging/index'),
  (15126, '账龄查看', 'fin:receivable:query', 3, 1, 15125, '', ''),
  (15127, '公告通知', '', 2, 1, 12, 'notice', 'more/notice/index'),
  (15128, '公告查看', 'infra:notice:query', 3, 1, 15127, '', ''),
  (15129, '公告发布', 'infra:notice:create', 3, 2, 15127, '', ''),
  (15130, '公告撤回', 'infra:notice:delete', 3, 3, 15127, '', ''),
  (15131, '操作日志', '', 2, 3, 1, 'oper-log', 'system/oper-log/index'),
  (15132, '日志查询', 'system:operlog:query', 3, 1, 15131, '', '');

-- 单号规则
INSERT INTO bas_serial_rule (biz_type, prefix, date_format, seq_length, reset_type, remark) VALUES
  ('finance_expense', 'FY', 'yyyyMM', 4, 'month', '费用登记');

-- ---------------------------------------------------------------------
-- 凭证模板：退货 / 盘点盈亏
-- ---------------------------------------------------------------------
INSERT INTO erp_voucher_template (biz_type, event, subject_id, direction, summary, amount_source, enabled) VALUES
  ('sale_return',    'audit', 6, 'debit',  '销售退货冲减收入', 'TOTAL', 1),
  ('sale_return',    'audit', 2, 'credit', '退货退款/冲应收', 'TOTAL', 1),
  ('purchase_return','audit', 5, 'debit',  '采购退货冲应付',   'TOTAL', 1),
  ('purchase_return','audit', 4, 'credit', '退回库存商品',     'TOTAL', 1),
  ('stock_check_gain','audit', 4, 'debit', '盘盈入账',         'TOTAL', 1),
  ('stock_check_gain','audit', 6, 'credit','盘盈营业外收入',   'TOTAL', 1),
  ('stock_check_loss','audit', 7, 'debit', '盘亏损失',         'TOTAL', 1),
  ('stock_check_loss','audit', 4, 'credit','盘亏转出库存',     'TOTAL', 1);

-- ---------------------------------------------------------------------
-- 菜单（id 段 15090+）
-- ---------------------------------------------------------------------
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15090, '库存调拨', '', 2, 3, 5, 'transfer', 'stk/transfer/index'),
  (15091, '调拨查询', 'stk:transfer:query', 3, 1, 15090, '', ''),
  (15092, '调拨新增', 'stk:transfer:create', 3, 2, 15090, '', ''),
  (15093, '调拨删除', 'stk:transfer:delete', 3, 3, 15090, '', ''),
  (15094, '调拨审核', 'stk:transfer:audit', 3, 4, 15090, '', ''),
  (15095, '调拨作废', 'stk:transfer:void', 3, 5, 15090, '', ''),
  (15096, '库存盘点', '', 2, 4, 5, 'check', 'stk/check/index'),
  (15097, '盘点查询', 'stk:check:query', 3, 1, 15096, '', ''),
  (15098, '盘点新增', 'stk:check:create', 3, 2, 15096, '', ''),
  (15099, '盘点删除', 'stk:check:delete', 3, 3, 15096, '', ''),
  (15100, '盘点审核', 'stk:check:audit', 3, 4, 15096, '', ''),
  (15101, '库存预警', '', 2, 5, 5, 'alert', 'stk/alert/index'),
  (15102, '预警查看', 'stk:quant:query', 3, 1, 15101, '', ''),
  (15103, '销售退货', '', 2, 3, 4, 'return', 'sal/return/index'),
  (15104, '退货查询', 'sal:return:query', 3, 1, 15103, '', ''),
  (15105, '退货新增', 'sal:return:create', 3, 2, 15103, '', ''),
  (15106, '退货删除', 'sal:return:delete', 3, 3, 15103, '', ''),
  (15107, '退货审核', 'sal:return:audit', 3, 4, 15103, '', ''),
  (15108, '退货作废', 'sal:return:void', 3, 5, 15103, '', ''),
  (15109, '采购退货', '', 2, 3, 3, 'return', 'pur/return/index'),
  (15110, '退货查询', 'pur:return:query', 3, 1, 15109, '', ''),
  (15111, '退货新增', 'pur:return:create', 3, 2, 15109, '', ''),
  (15112, '退货删除', 'pur:return:delete', 3, 3, 15109, '', ''),
  (15113, '退货审核', 'pur:return:audit', 3, 4, 15109, '', ''),
  (15114, '退货作废', 'pur:return:void', 3, 5, 15109, '', ''),
  (15115, '其他出入库', '', 2, 6, 5, 'other-stock', 'stk/other-stock/index'),
  (15116, '其他出入库查询', 'stk:other:query', 3, 1, 15115, '', ''),
  (15117, '其他出入库新增', 'stk:other:create', 3, 2, 15115, '', ''),
  (15118, '其他出入库删除', 'stk:other:delete', 3, 3, 15115, '', ''),
  (15119, '其他出入库审核', 'stk:other:audit', 3, 4, 15115, '', '');

-- 单号规则
INSERT INTO bas_serial_rule (biz_type, prefix, date_format, seq_length, reset_type, remark) VALUES
  ('stock_transfer',   'TF', 'yyyyMM', 4, 'month', '库存调拨'),
  ('stock_check',      'CK', 'yyyyMM', 4, 'month', '库存盘点'),
  ('sale_return',      'SR', 'yyyyMM', 4, 'month', '销售退货'),
  ('purchase_return',  'PR', 'yyyyMM', 4, 'month', '采购退货'),
  ('stock_other_in',   'QI', 'yyyyMM', 4, 'month', '其他入库'),
  ('stock_other_out',  'QO', 'yyyyMM', 4, 'month', '其他出库');
