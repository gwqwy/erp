-- =====================================================================
-- 天工 ERP V2：M1 进销存闭环
-- 采购/销售单据 + 库存双账（move/quant）+ biz_chatter + 菜单 + 单号规则
-- =====================================================================

-- ---------------------------------------------------------------------
-- 采购域
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_purchase_order (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  no               VARCHAR(32)   NOT NULL COMMENT '单号 POyyyyMMxxxx',
  supplier_id      BIGINT        NOT NULL COMMENT '供应商 erp_partner',
  order_date       DATE          NOT NULL,
  expect_date      DATE                   COMMENT '期望到货日',
  total_qty        DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_amount     DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '不含税合计（M1=价税合计）',
  total_tax        DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_amount_tax DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '价税合计',
  remark           VARCHAR(500),
  status           TINYINT       NOT NULL DEFAULT 10 COMMENT '10草稿 30已审核 40已完成 50已作废',
  audit_user_id    BIGINT,
  audit_time       DATETIME,
  creator          VARCHAR(64)   NOT NULL DEFAULT '',
  create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater          VARCHAR(64)   NOT NULL DEFAULT '',
  update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id        BIGINT        NOT NULL DEFAULT 1,
  company_id       BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_supplier (supplier_id),
  KEY idx_status_date (status, order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '采购订单';

CREATE TABLE IF NOT EXISTS erp_purchase_order_item (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  order_id    BIGINT        NOT NULL,
  product_id  BIGINT        NOT NULL,
  unit_id     BIGINT,
  qty         DECIMAL(20,6) NOT NULL,
  price       DECIMAL(20,6) NOT NULL COMMENT '含税单价',
  amount      DECIMAL(20,6) NOT NULL DEFAULT 0,
  tax_rate    DECIMAL(10,6) NOT NULL DEFAULT 0,
  inbound_qty DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已入库数量（回写）',
  remark      VARCHAR(255),
  creator     VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id   BIGINT        NOT NULL DEFAULT 1,
  company_id  BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '采购订单明细';

CREATE TABLE IF NOT EXISTS erp_purchase_inbound (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  no            VARCHAR(32)   NOT NULL COMMENT '单号 PIyyyyMMxxxx',
  supplier_id   BIGINT        NOT NULL,
  warehouse_id  BIGINT        NOT NULL,
  order_id      BIGINT                 COMMENT '来源采购订单，可空',
  inbound_date  DATE          NOT NULL,
  total_qty     DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_amount  DECIMAL(20,6) NOT NULL DEFAULT 0,
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
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_order (order_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '采购入库单';

CREATE TABLE IF NOT EXISTS erp_purchase_inbound_item (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  inbound_id   BIGINT        NOT NULL,
  order_item_id BIGINT                COMMENT '来源订单明细，可空',
  product_id   BIGINT        NOT NULL,
  unit_id      BIGINT,
  qty          DECIMAL(20,6) NOT NULL,
  price        DECIMAL(20,6) NOT NULL DEFAULT 0,
  amount       DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark       VARCHAR(255),
  creator      VARCHAR(64)   NOT NULL DEFAULT '',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)   NOT NULL DEFAULT '',
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id    BIGINT        NOT NULL DEFAULT 1,
  company_id   BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_inbound (inbound_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '采购入库明细';

-- ---------------------------------------------------------------------
-- 销售域
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_sale_order (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  no               VARCHAR(32)   NOT NULL COMMENT '单号 SOyyyyMMxxxx',
  customer_id      BIGINT        NOT NULL COMMENT '客户 erp_partner',
  order_date       DATE          NOT NULL,
  expect_date      DATE                   COMMENT '期望交期',
  total_qty        DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_amount     DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_tax        DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_amount_tax DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark           VARCHAR(500),
  status           TINYINT       NOT NULL DEFAULT 10,
  audit_user_id    BIGINT,
  audit_time       DATETIME,
  creator          VARCHAR(64)   NOT NULL DEFAULT '',
  create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater          VARCHAR(64)   NOT NULL DEFAULT '',
  update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id        BIGINT        NOT NULL DEFAULT 1,
  company_id       BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_customer (customer_id),
  KEY idx_status_date (status, order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '销售订单';

CREATE TABLE IF NOT EXISTS erp_sale_order_item (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  order_id     BIGINT        NOT NULL,
  product_id   BIGINT        NOT NULL,
  unit_id      BIGINT,
  qty          DECIMAL(20,6) NOT NULL,
  price        DECIMAL(20,6) NOT NULL COMMENT '含税单价',
  amount       DECIMAL(20,6) NOT NULL DEFAULT 0,
  tax_rate     DECIMAL(10,6) NOT NULL DEFAULT 0,
  outbound_qty DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已出库数量（回写）',
  remark       VARCHAR(255),
  creator      VARCHAR(64)   NOT NULL DEFAULT '',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)   NOT NULL DEFAULT '',
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id    BIGINT        NOT NULL DEFAULT 1,
  company_id   BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '销售订单明细';

CREATE TABLE IF NOT EXISTS erp_sale_outbound (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  no            VARCHAR(32)   NOT NULL COMMENT '单号 SDyyyyMMxxxx',
  customer_id   BIGINT        NOT NULL,
  warehouse_id  BIGINT        NOT NULL,
  order_id      BIGINT                 COMMENT '来源销售订单，可空',
  outbound_date DATE          NOT NULL,
  total_qty     DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_amount  DECIMAL(20,6) NOT NULL DEFAULT 0,
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
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_order (order_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '销售出库单';

CREATE TABLE IF NOT EXISTS erp_sale_outbound_item (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  outbound_id  BIGINT        NOT NULL,
  order_item_id BIGINT                COMMENT '来源订单明细，可空',
  product_id   BIGINT        NOT NULL,
  unit_id      BIGINT,
  qty          DECIMAL(20,6) NOT NULL,
  price        DECIMAL(20,6) NOT NULL DEFAULT 0,
  amount       DECIMAL(20,6) NOT NULL DEFAULT 0,
  remark       VARCHAR(255),
  creator      VARCHAR(64)   NOT NULL DEFAULT '',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)   NOT NULL DEFAULT '',
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id    BIGINT        NOT NULL DEFAULT 1,
  company_id   BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_outbound (outbound_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '销售出库明细';

-- ---------------------------------------------------------------------
-- 库存双账
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_stock_move (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  move_no      VARCHAR(32)   NOT NULL COMMENT '流水号 MVyyyyMMxxxx',
  biz_type     VARCHAR(32)   NOT NULL COMMENT 'purchase_in/sale_out/…',
  biz_id       BIGINT        NOT NULL,
  biz_no       VARCHAR(32),
  direction    TINYINT       NOT NULL COMMENT '1 入库 -1 出库',
  warehouse_id BIGINT        NOT NULL,
  location_id  BIGINT        NOT NULL DEFAULT 0 COMMENT '库位，M1 默认 0',
  product_id   BIGINT        NOT NULL,
  unit_id      BIGINT,
  qty          DECIMAL(20,6) NOT NULL COMMENT '基本单位数量，带方向',
  price        DECIMAL(20,6)          COMMENT '成本单价',
  amount       DECIMAL(20,6)          COMMENT '成本金额',
  batch_no     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '批次，无批次空串',
  partner_id   BIGINT,
  move_time    DATETIME      NOT NULL,
  post_user_id BIGINT,
  creator      VARCHAR(64)   NOT NULL DEFAULT '',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)   NOT NULL DEFAULT '',
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id    BIGINT        NOT NULL DEFAULT 1,
  company_id   BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_product_time (product_id, move_time),
  KEY idx_biz (biz_type, biz_id),
  KEY idx_wh_time (warehouse_id, move_time),
  UNIQUE KEY uk_move_no (move_no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '库存流水账（只增不改）';

CREATE TABLE IF NOT EXISTS erp_stock_quant (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  company_id   BIGINT        NOT NULL DEFAULT 1,
  warehouse_id BIGINT        NOT NULL,
  location_id  BIGINT        NOT NULL DEFAULT 0,
  product_id   BIGINT        NOT NULL,
  batch_no     VARCHAR(64)   NOT NULL DEFAULT '',
  qty          DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '当前结存（基本单位）',
  locked_qty   DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '占用量（预留）',
  avg_cost     DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '移动加权平均成本',
  version      INT           NOT NULL DEFAULT 0 COMMENT '乐观锁',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dim (company_id, warehouse_id, location_id, product_id, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '即时库存账';

-- ---------------------------------------------------------------------
-- Chatter
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_chatter (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  biz_type    VARCHAR(32)  NOT NULL COMMENT '如 pur_order',
  biz_id      BIGINT       NOT NULL,
  msg_type    VARCHAR(16)  NOT NULL COMMENT 'comment/system_trace',
  content     TEXT,
  parent_id   BIGINT,
  user_id     BIGINT COMMENT '发布人，系统轨迹可空',
  extra       JSON,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '单据记录动态（借鉴 Odoo Chatter）';

-- ---------------------------------------------------------------------
-- 菜单：采购管理(3) / 销售管理(4) / 库存管理(5)
-- ---------------------------------------------------------------------
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (3,    '采购管理', '', 1, 3, 0, '/pur', ''),
  (300,  '采购订单', '', 2, 1, 3, 'order', 'pur/order/index'),
  (3001, '订单查询', 'pur:order:query', 3, 1, 300, '', ''),
  (3002, '订单新增', 'pur:order:create', 3, 2, 300, '', ''),
  (3003, '订单修改', 'pur:order:update', 3, 3, 300, '', ''),
  (3004, '订单删除', 'pur:order:delete', 3, 4, 300, '', ''),
  (3005, '订单审核', 'pur:order:audit', 3, 5, 300, '', ''),
  (3006, '订单作废', 'pur:order:void', 3, 6, 300, '', ''),
  (301,  '采购入库', '', 2, 2, 3, 'inbound', 'pur/inbound/index'),
  (3011, '入库查询', 'pur:inbound:query', 3, 1, 301, '', ''),
  (3012, '入库新增', 'pur:inbound:create', 3, 2, 301, '', ''),
  (3013, '入库删除', 'pur:inbound:delete', 3, 3, 301, '', ''),
  (3014, '入库审核', 'pur:inbound:audit', 3, 4, 301, '', ''),
  (3015, '入库作废', 'pur:inbound:void', 3, 5, 301, '', ''),
  (4,    '销售管理', '', 1, 4, 0, '/sal', ''),
  (400,  '销售订单', '', 2, 1, 4, 'order', 'sal/order/index'),
  (4001, '订单查询', 'sal:order:query', 3, 1, 400, '', ''),
  (4002, '订单新增', 'sal:order:create', 3, 2, 400, '', ''),
  (4003, '订单修改', 'sal:order:update', 3, 3, 400, '', ''),
  (4004, '订单删除', 'sal:order:delete', 3, 4, 400, '', ''),
  (4005, '订单审核', 'sal:order:audit', 3, 5, 400, '', ''),
  (4006, '订单作废', 'sal:order:void', 3, 6, 400, '', ''),
  (401,  '销售出库', '', 2, 2, 4, 'outbound', 'sal/outbound/index'),
  (4011, '出库查询', 'sal:outbound:query', 3, 1, 401, '', ''),
  (4012, '出库新增', 'sal:outbound:create', 3, 2, 401, '', ''),
  (4013, '出库删除', 'sal:outbound:delete', 3, 3, 401, '', ''),
  (4014, '出库审核', 'sal:outbound:audit', 3, 4, 401, '', ''),
  (4015, '出库作废', 'sal:outbound:void', 3, 5, 401, '', ''),
  (5,    '库存管理', '', 1, 5, 0, '/stk', ''),
  (500,  '即时库存', '', 2, 1, 5, 'quant', 'stk/quant/index'),
  (5001, '库存查询', 'stk:quant:query', 3, 1, 500, '', ''),
  (501,  '库存流水', '', 2, 2, 5, 'move', 'stk/move/index'),
  (5011, '流水查询', 'stk:move:query', 3, 1, 501, '', '');

-- ---------------------------------------------------------------------
-- 单号规则
-- ---------------------------------------------------------------------
INSERT INTO bas_serial_rule (biz_type, prefix, date_format, seq_length, reset_type, remark) VALUES
  ('purchase_inbound', 'PI', 'yyyyMM', 4, 'month', '采购入库单'),
  ('sale_outbound',    'SD', 'yyyyMM', 4, 'month', '销售出库单');
