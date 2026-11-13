-- =====================================================================
-- V11：全功能补全 —— 发票 / 库位 / 站内信 / 价格策略 / HR三表 / 回款计划 / 期间锁账 / 批次效期 / 委外 / 预收预付
-- =====================================================================

ALTER TABLE erp_stock_quant ADD COLUMN expire_date DATE NULL COMMENT '效期至';
ALTER TABLE erp_mrp_work_order ADD COLUMN outsourced BIT(1) NOT NULL DEFAULT b'0' COMMENT '委外';
ALTER TABLE erp_mrp_work_order ADD COLUMN supplier_id BIGINT NULL COMMENT '委外供应商';
ALTER TABLE erp_finance_receipt ADD COLUMN advance BIT(1) NOT NULL DEFAULT b'0' COMMENT '预收';
ALTER TABLE erp_finance_payment ADD COLUMN advance BIT(1) NOT NULL DEFAULT b'0' COMMENT '预付';

CREATE TABLE IF NOT EXISTS erp_location (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  warehouse_id BIGINT      NOT NULL,
  no           VARCHAR(32) NOT NULL,
  name         VARCHAR(64) NOT NULL,
  status       TINYINT     NOT NULL DEFAULT 0,
  creator      VARCHAR(64)  NOT NULL DEFAULT '',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)  NOT NULL DEFAULT '',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id    BIGINT       NOT NULL DEFAULT 1,
  company_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_wh (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '库位';

CREATE TABLE IF NOT EXISTS erp_finance_invoice (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  no           VARCHAR(32)   NOT NULL COMMENT '登记号 FPyyyyMMxxxx',
  type         VARCHAR(8)    NOT NULL COMMENT 'sale 销项 / purchase 进项',
  invoice_no   VARCHAR(64)   NOT NULL COMMENT '发票号码',
  invoice_date DATE          NOT NULL,
  order_id     BIGINT                 COMMENT '关联订单，可空',
  amount       DECIMAL(20,6) NOT NULL COMMENT '价税合计',
  tax_amount   DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '税额',
  remark       VARCHAR(500),
  creator      VARCHAR(64)   NOT NULL DEFAULT '',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)   NOT NULL DEFAULT '',
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id    BIGINT        NOT NULL DEFAULT 1,
  company_id   BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '发票登记';

CREATE TABLE IF NOT EXISTS system_message (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL COMMENT '接收人',
  title       VARCHAR(128) NOT NULL,
  content     VARCHAR(500),
  read_flag   BIT(1)       NOT NULL DEFAULT b'0' COMMENT '0 未读 1 已读',
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user (user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '站内消息';

CREATE TABLE IF NOT EXISTS erp_partner_price (
  id         BIGINT        NOT NULL AUTO_INCREMENT,
  partner_id BIGINT        NOT NULL,
  product_id BIGINT        NOT NULL,
  price      DECIMAL(20,6) NOT NULL,
  remark     VARCHAR(255),
  creator    VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater    VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id  BIGINT        NOT NULL DEFAULT 1,
  company_id BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pp (partner_id, product_id, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '客户/供应商价格策略';

CREATE TABLE IF NOT EXISTS hr_recruit (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)  NOT NULL,
  position    VARCHAR(64)  NOT NULL COMMENT '岗位',
  headcount   INT          NOT NULL DEFAULT 1 COMMENT '招聘人数',
  department  VARCHAR(64),
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 招聘中 1 已完成 2 已取消',
  remark      VARCHAR(500),
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '招聘需求';

CREATE TABLE IF NOT EXISTS hr_training (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)  NOT NULL,
  title       VARCHAR(128) NOT NULL,
  trainer     VARCHAR(64),
  train_date  DATE,
  employees   VARCHAR(500) COMMENT '参训员工（文本）',
  remark      VARCHAR(500),
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '培训记录';

CREATE TABLE IF NOT EXISTS hr_performance (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)   NOT NULL,
  employee_id BIGINT        NOT NULL,
  period      VARCHAR(7)    NOT NULL COMMENT '考核期 yyyy-MM',
  score       DECIMAL(6,2)  NOT NULL DEFAULT 0 COMMENT '得分',
  grade       VARCHAR(8)             COMMENT 'S/A/B/C',
  remark      VARCHAR(500),
  creator     VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id   BIGINT        NOT NULL DEFAULT 1,
  company_id  BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '绩效考核';

CREATE TABLE IF NOT EXISTS erp_finance_receivable_plan (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)   NOT NULL,
  partner_id  BIGINT        NOT NULL,
  plan_date   DATE          NOT NULL,
  amount      DECIMAL(20,6) NOT NULL,
  status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0 计划 1 已回款',
  remark      VARCHAR(500),
  creator     VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id   BIGINT        NOT NULL DEFAULT 1,
  company_id  BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '回款计划';

CREATE TABLE IF NOT EXISTS erp_finance_period (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  period     VARCHAR(7)  NOT NULL COMMENT 'yyyy-MM',
  closed     BIT(1)      NOT NULL DEFAULT b'0',
  close_time DATETIME,
  close_user VARCHAR(32),
  PRIMARY KEY (id),
  UNIQUE KEY uk_period (period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '会计期间锁账';

INSERT INTO erp_subject (id, no, name, direction, status) VALUES (11, '4103', '本年利润', -1, 0);

INSERT INTO bas_serial_rule (biz_type, prefix, date_format, seq_length, reset_type, remark) VALUES
  ('finance_invoice', 'FP', 'yyyyMM', 4, 'month', '发票登记'),
  ('hr_recruit',      'ZP', 'yyyyMM', 4, 'month', '招聘'),
  ('hr_training',     'PX', 'yyyyMM', 4, 'month', '培训'),
  ('hr_performance',  'JX', 'yyyyMM', 4, 'month', '绩效'),
  ('hr_receivable_plan', 'HK', 'yyyyMM', 4, 'month', '回款计划');

-- ---------------------------------------------------------------------
-- 菜单（id 段 15140+）
-- ---------------------------------------------------------------------
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15140, '发票登记', '', 2, 9, 6, 'invoice', 'fin/invoice/index'),
  (15141, '发票查询', 'fin:invoice:query', 3, 1, 15140, '', ''),
  (15142, '发票登记', 'fin:invoice:create', 3, 2, 15140, '', ''),
  (15143, '发票删除', 'fin:invoice:delete', 3, 3, 15140, '', ''),
  (15144, '回款计划', '', 2, 9, 6, 'receivable-plan', 'fin/receivable-plan/index'),
  (15145, '回款计划查询', 'fin:receivableplan:query', 3, 1, 15144, '', ''),
  (15146, '回款计划登记', 'fin:receivableplan:create', 3, 2, 15144, '', ''),
  (15147, '回款计划删除', 'fin:receivableplan:delete', 3, 3, 15144, '', ''),
  (15148, '期末结转', '', 2, 10, 6, 'period-close', 'fin/period-close/index'),
  (15149, '结转操作', 'fin:periodclose:operate', 3, 1, 15148, '', ''),
  (15150, '库位管理', '', 2, 7, 2, 'location', 'bas/location/index'),
  (15151, '库位查询', 'bas:location:query', 3, 1, 15150, '', ''),
  (15152, '库位新增', 'bas:location:create', 3, 2, 15150, '', ''),
  (15153, '库位修改', 'bas:location:update', 3, 3, 15150, '', ''),
  (15154, '库位删除', 'bas:location:delete', 3, 4, 15150, '', ''),
  (15155, '价格策略', '', 2, 6, 2, 'partner-price', 'bas/partner-price/index'),
  (15156, '价格查询', 'bas:price:query', 3, 1, 15155, '', ''),
  (15157, '价格新增', 'bas:price:create', 3, 2, 15155, '', ''),
  (15158, '价格删除', 'bas:price:delete', 3, 3, 15155, '', ''),
  (15159, '招聘管理', '', 2, 12, 9, 'recruit', 'hr/recruit/index'),
  (15160, '招聘查询', 'hr:recruit:query', 3, 1, 15159, '', ''),
  (15161, '招聘新增', 'hr:recruit:create', 3, 2, 15159, '', ''),
  (15162, '招聘删除', 'hr:recruit:delete', 3, 3, 15159, '', ''),
  (15163, '培训记录', '', 2, 13, 9, 'training', 'hr/training/index'),
  (15164, '培训查询', 'hr:training:query', 3, 1, 15163, '', ''),
  (15165, '培训新增', 'hr:training:create', 3, 2, 15163, '', ''),
  (15166, '培训删除', 'hr:training:delete', 3, 3, 15163, '', ''),
  (15167, '绩效考核', '', 2, 14, 9, 'performance', 'hr/performance/index'),
  (15168, '绩效查询', 'hr:performance:query', 3, 1, 15167, '', ''),
  (15169, '绩效登记', 'hr:performance:create', 3, 2, 15167, '', ''),
  (15170, '绩效删除', 'hr:performance:delete', 3, 3, 15167, '', ''),
  (15171, '消息中心', '', 2, 1, 12, 'message', 'more/message/index'),
  (15172, '消息查看', 'infra:message:query', 3, 1, 15171, '', ''),
  (15173, '消息发送', 'infra:message:send', 3, 2, 15171, '', ''),
  (15174, '回收站', '', 2, 5, 12, 'recycle', 'more/recycle/index'),
  (15175, '回收站查看', 'infra:recycle:query', 3, 1, 15174, '', ''),
  (15176, '回收站恢复', 'infra:recycle:restore', 3, 2, 15174, '', '');

-- CSV 导出权限
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES (15180, 'CSV导出', 'infra:export:csv', 3, 1, 15174, '', '');
