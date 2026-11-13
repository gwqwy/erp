-- =====================================================================
-- 天工 ERP V3：M2 业务财务一体化
-- 科目/结算账户 + 应收应付台账 + 收付款单 + 总账凭证 + 凭证模板 + 菜单
-- =====================================================================

-- ---------------------------------------------------------------------
-- 基础财务数据
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_subject (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  no          VARCHAR(16)  NOT NULL COMMENT '科目编码',
  name        VARCHAR(64)  NOT NULL COMMENT '科目名称',
  direction   TINYINT      NOT NULL COMMENT '1 借方科目 -1 贷方科目',
  status      TINYINT      NOT NULL DEFAULT 0,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '会计科目（M2 平面表）';

CREATE TABLE IF NOT EXISTS erp_finance_account (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32),
  name        VARCHAR(64)   NOT NULL COMMENT '账户名称',
  type        TINYINT       NOT NULL DEFAULT 2 COMMENT '1 现金 2 银行 3 其他',
  balance     DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '余额（收付款联动）',
  status      TINYINT       NOT NULL DEFAULT 0,
  remark      VARCHAR(255),
  creator     VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id   BIGINT        NOT NULL DEFAULT 1,
  company_id  BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '结算账户';

-- ---------------------------------------------------------------------
-- 应收应付台账
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_finance_receivable (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  biz_type        VARCHAR(32)   NOT NULL COMMENT '来源业务：sale_out 等',
  biz_id          BIGINT        NOT NULL,
  biz_no          VARCHAR(32),
  partner_id      BIGINT        NOT NULL COMMENT '客户',
  amount          DECIMAL(20,6) NOT NULL COMMENT '应收金额',
  received_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已核销金额',
  due_date        DATE,
  status          TINYINT       NOT NULL DEFAULT 1 COMMENT '1 未核销 2 部分 3 已结清 4 作废',
  settle_date     DATE,
  creator         VARCHAR(64)   NOT NULL DEFAULT '',
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater         VARCHAR(64)   NOT NULL DEFAULT '',
  update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id       BIGINT        NOT NULL DEFAULT 1,
  company_id      BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_partner_status (partner_id, status),
  KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '应收台账';

CREATE TABLE IF NOT EXISTS erp_finance_payable (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  biz_type       VARCHAR(32)   NOT NULL COMMENT '来源业务：purchase_in 等',
  biz_id         BIGINT        NOT NULL,
  biz_no         VARCHAR(32),
  partner_id     BIGINT        NOT NULL COMMENT '供应商',
  amount         DECIMAL(20,6) NOT NULL COMMENT '应付金额',
  paid_amount    DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已核销金额',
  due_date       DATE,
  status         TINYINT       NOT NULL DEFAULT 1 COMMENT '1 未核销 2 部分 3 已结清 4 作废',
  settle_date    DATE,
  creator        VARCHAR(64)   NOT NULL DEFAULT '',
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater        VARCHAR(64)   NOT NULL DEFAULT '',
  update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id      BIGINT        NOT NULL DEFAULT 1,
  company_id     BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_partner_status (partner_id, status),
  KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '应付台账';

-- ---------------------------------------------------------------------
-- 收付款单
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_finance_receipt (
  id                  BIGINT        NOT NULL AUTO_INCREMENT,
  no                  VARCHAR(32)   NOT NULL COMMENT '单号 SKyyyyMMxxxx',
  partner_id          BIGINT        NOT NULL,
  account_id          BIGINT        NOT NULL COMMENT '收款结算账户',
  receipt_date        DATE          NOT NULL,
  amount              DECIMAL(20,6) NOT NULL COMMENT '收款金额 = Σ核销明细',
  unallocated_amount  DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark              VARCHAR(500),
  status              TINYINT       NOT NULL DEFAULT 10,
  audit_user_id       BIGINT,
  audit_time          DATETIME,
  creator             VARCHAR(64)   NOT NULL DEFAULT '',
  create_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater             VARCHAR(64)   NOT NULL DEFAULT '',
  update_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted             BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id           BIGINT        NOT NULL DEFAULT 1,
  company_id          BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_partner (partner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '收款单';

CREATE TABLE IF NOT EXISTS erp_finance_receipt_item (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  receipt_id    BIGINT        NOT NULL,
  receivable_id BIGINT        NOT NULL COMMENT '被核销应收',
  amount        DECIMAL(20,6) NOT NULL COMMENT '本次核销金额',
  remark        VARCHAR(255),
  creator       VARCHAR(64)   NOT NULL DEFAULT '',
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater       VARCHAR(64)   NOT NULL DEFAULT '',
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id     BIGINT        NOT NULL DEFAULT 1,
  company_id    BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_receipt (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '收款核销明细';

CREATE TABLE IF NOT EXISTS erp_finance_payment (
  id                  BIGINT        NOT NULL AUTO_INCREMENT,
  no                  VARCHAR(32)   NOT NULL COMMENT '单号 FKyyyyMMxxxx',
  partner_id          BIGINT        NOT NULL,
  account_id          BIGINT        NOT NULL COMMENT '付款结算账户',
  payment_date        DATE          NOT NULL,
  amount              DECIMAL(20,6) NOT NULL COMMENT '付款金额 = Σ核销明细',
  unallocated_amount  DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark              VARCHAR(500),
  status              TINYINT       NOT NULL DEFAULT 10,
  audit_user_id       BIGINT,
  audit_time          DATETIME,
  creator             VARCHAR(64)   NOT NULL DEFAULT '',
  create_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater             VARCHAR(64)   NOT NULL DEFAULT '',
  update_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted             BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id           BIGINT        NOT NULL DEFAULT 1,
  company_id          BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_partner (partner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '付款单';

CREATE TABLE IF NOT EXISTS erp_finance_payment_item (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  payment_id  BIGINT        NOT NULL,
  payable_id  BIGINT        NOT NULL COMMENT '被核销应付',
  amount      DECIMAL(20,6) NOT NULL COMMENT '本次核销金额',
  remark      VARCHAR(255),
  creator     VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id   BIGINT        NOT NULL DEFAULT 1,
  company_id  BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_payment (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '付款核销明细';

-- ---------------------------------------------------------------------
-- 总账凭证 + 模板
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_finance_voucher (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  no           VARCHAR(32)   NOT NULL COMMENT '凭证字 PZyyyyMMxxxx',
  voucher_date DATE          NOT NULL,
  period       VARCHAR(7)    NOT NULL COMMENT '会计期间 yyyy-MM',
  biz_type     VARCHAR(32)            COMMENT '来源业务类型（手工凭证为空）',
  biz_id       BIGINT,
  source_no    VARCHAR(32),
  total_debit  DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_credit DECIMAL(20,6) NOT NULL DEFAULT 0,
  status       TINYINT       NOT NULL DEFAULT 2 COMMENT '1 草稿 2 已过账 3 已作废',
  posted_time  DATETIME,
  creator      VARCHAR(64)   NOT NULL DEFAULT '',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)   NOT NULL DEFAULT '',
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id    BIGINT        NOT NULL DEFAULT 1,
  company_id   BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_biz (biz_type, biz_id),
  KEY idx_period (period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '总账凭证';

CREATE TABLE IF NOT EXISTS erp_finance_voucher_item (
  id         BIGINT        NOT NULL AUTO_INCREMENT,
  voucher_id BIGINT        NOT NULL,
  subject_id BIGINT        NOT NULL,
  subject_no VARCHAR(16),
  summary    VARCHAR(255),
  debit      DECIMAL(20,6) NOT NULL DEFAULT 0,
  credit     DECIMAL(20,6) NOT NULL DEFAULT 0,
  partner_id BIGINT,
  creator    VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater    VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id  BIGINT        NOT NULL DEFAULT 1,
  company_id BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_voucher (voucher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '凭证分录';

CREATE TABLE IF NOT EXISTS erp_voucher_template (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  biz_type     VARCHAR(32)  NOT NULL COMMENT 'sale_out/purchase_in/receipt/payment',
  event        VARCHAR(32)  NOT NULL DEFAULT 'audit',
  subject_id   BIGINT       NOT NULL COMMENT '科目',
  direction    VARCHAR(8)   NOT NULL COMMENT 'debit/credit',
  summary      VARCHAR(255) COMMENT '分录摘要',
  amount_source VARCHAR(16) NOT NULL DEFAULT 'TOTAL' COMMENT 'M2 固定 TOTAL',
  enabled      TINYINT      NOT NULL DEFAULT 1,
  creator      VARCHAR(64)  NOT NULL DEFAULT '',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)  NOT NULL DEFAULT '',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id    BIGINT       NOT NULL DEFAULT 1,
  company_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_biz_event (biz_type, event)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '凭证模板（页面可配置）';

-- ---------------------------------------------------------------------
-- 种子数据
-- ---------------------------------------------------------------------
INSERT INTO erp_subject (id, no, name, direction, status) VALUES
  (1, '1001', '库存现金',     1, 0),
  (2, '1002', '银行存款',     1, 0),
  (3, '1122', '应收账款',     1, 0),
  (4, '1405', '库存商品',     1, 0),
  (5, '2202', '应付账款',    -1, 0),
  (6, '6001', '主营业务收入', -1, 0),
  (7, '6401', '主营业务成本', 1, 0),
  (8, '2221', '应交税费',    -1, 0);

INSERT INTO erp_finance_account (id, no, name, type, balance, status, remark) VALUES
  (1, 'BANK01', '基本户（工商银行）', 2, 0, 0, '默认结算账户'),
  (2, 'CASH01', '备用金（现金）',     1, 0, 0, NULL);

-- 凭证模板：借方合计 = 贷方合计（税金 M2 简化不拆分）
INSERT INTO erp_voucher_template (biz_type, event, subject_id, direction, summary, amount_source, enabled) VALUES
  ('sale_out',    'audit', 3, 'debit',  '赊销应收',      'TOTAL', 1),
  ('sale_out',    'audit', 6, 'credit', '确认销售收入',  'TOTAL', 1),
  ('purchase_in', 'audit', 4, 'debit',  '采购入库',      'TOTAL', 1),
  ('purchase_in', 'audit', 5, 'credit', '赊购应付',      'TOTAL', 1),
  ('receipt',     'audit', 2, 'debit',  '收款入账',      'TOTAL', 1),
  ('receipt',     'audit', 3, 'credit', '核销应收',      'TOTAL', 1),
  ('payment',     'audit', 5, 'debit',  '核销应付',      'TOTAL', 1),
  ('payment',     'audit', 2, 'credit', '付款出账',      'TOTAL', 1);

-- ---------------------------------------------------------------------
-- 菜单：财务管理(6)
-- ---------------------------------------------------------------------
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (6,    '财务管理', '', 1, 6, 0, '/fin', ''),
  (600,  '应收台账', '', 2, 1, 6, 'receivable', 'fin/receivable/index'),
  (6001, '应收查询', 'fin:receivable:query', 3, 1, 600, '', ''),
  (601,  '应付台账', '', 2, 2, 6, 'payable', 'fin/payable/index'),
  (6011, '应付查询', 'fin:payable:query', 3, 1, 601, '', ''),
  (602,  '收款单', '', 2, 3, 6, 'receipt', 'fin/receipt/index'),
  (6021, '收款查询', 'fin:receipt:query', 3, 1, 602, '', ''),
  (6022, '收款新增', 'fin:receipt:create', 3, 2, 602, '', ''),
  (6023, '收款删除', 'fin:receipt:delete', 3, 3, 602, '', ''),
  (6024, '收款审核', 'fin:receipt:audit', 3, 4, 602, '', ''),
  (6025, '收款作废', 'fin:receipt:void', 3, 5, 602, '', ''),
  (603,  '付款单', '', 2, 4, 6, 'payment', 'fin/payment/index'),
  (6031, '付款查询', 'fin:payment:query', 3, 1, 603, '', ''),
  (6032, '付款新增', 'fin:payment:create', 3, 2, 603, '', ''),
  (6033, '付款删除', 'fin:payment:delete', 3, 3, 603, '', ''),
  (6034, '付款审核', 'fin:payment:audit', 3, 4, 603, '', ''),
  (6035, '付款作废', 'fin:payment:void', 3, 5, 603, '', ''),
  (604,  '总账凭证', '', 2, 5, 6, 'voucher', 'fin/voucher/index'),
  (6041, '凭证查询', 'fin:voucher:query', 3, 1, 604, '', ''),
  (605,  '凭证模板', '', 2, 6, 6, 'voucher-template', 'fin/voucher-template/index'),
  (6051, '模板查询', 'fin:template:query', 3, 1, 605, '', ''),
  (6052, '模板新增', 'fin:template:create', 3, 2, 605, '', ''),
  (6053, '模板修改', 'fin:template:update', 3, 3, 605, '', ''),
  (6054, '模板删除', 'fin:template:delete', 3, 4, 605, '', '');

-- ---------------------------------------------------------------------
-- 单号规则
-- ---------------------------------------------------------------------
INSERT INTO bas_serial_rule (biz_type, prefix, date_format, seq_length, reset_type, remark) VALUES
  ('finance_receipt', 'SK', 'yyyyMM', 4, 'month', '收款单'),
  ('finance_payment', 'FK', 'yyyyMM', 4, 'month', '付款单'),
  ('finance_voucher', 'PZ', 'yyyyMM', 4, 'month', '记账凭证');
