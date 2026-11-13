-- =====================================================================
-- 天工 ERP V4：M3 生产制造
-- BOM 多版本 + 生产工单（材料需求展开）+ 领料单 + 菜单 + 单号规则
-- =====================================================================

CREATE TABLE IF NOT EXISTS erp_mrp_bom (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)   NOT NULL COMMENT 'BOM 编号',
  product_id  BIGINT        NOT NULL COMMENT '成品/半成品',
  version     INT           NOT NULL DEFAULT 1 COMMENT '版本',
  qty         DECIMAL(20,6) NOT NULL COMMENT '产出基数',
  status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0 草稿 1 启用',
  is_default  TINYINT       NOT NULL DEFAULT 0 COMMENT '1 为该产品默认 BOM',
  remark      VARCHAR(500),
  creator     VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id   BIGINT        NOT NULL DEFAULT 1,
  company_id  BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ver (product_id, version, company_id),
  KEY idx_status (status, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'BOM 主表';

CREATE TABLE IF NOT EXISTS erp_mrp_bom_item (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  bom_id      BIGINT        NOT NULL,
  product_id  BIGINT        NOT NULL COMMENT '组件产品',
  qty         DECIMAL(20,6) NOT NULL COMMENT '单耗（相对 BOM 基数）',
  loss_rate   DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT '损耗率 %',
  remark      VARCHAR(255),
  creator     VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id   BIGINT        NOT NULL DEFAULT 1,
  company_id  BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_bom (bom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'BOM 组件行';

CREATE TABLE IF NOT EXISTS erp_mrp_work_order (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  no               VARCHAR(32)   NOT NULL COMMENT '单号 MOyyyyMMxxxx',
  product_id       BIGINT        NOT NULL COMMENT '成品',
  bom_id           BIGINT        NOT NULL,
  warehouse_id     BIGINT        NOT NULL COMMENT '完工入库仓',
  qty              DECIMAL(20,6) NOT NULL COMMENT '计划数量',
  finished_qty     DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '累计完工',
  plan_start_date  DATE,
  plan_finish_date DATE,
  remark           VARCHAR(500),
  status           TINYINT       NOT NULL DEFAULT 10 COMMENT '10计划 30已下达 40生产中 50完成 60作废',
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
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '生产工单';

CREATE TABLE IF NOT EXISTS erp_mrp_work_order_item (
  id         BIGINT        NOT NULL AUTO_INCREMENT,
  work_order_id BIGINT     NOT NULL,
  product_id BIGINT        NOT NULL COMMENT '材料',
  unit_id    BIGINT,
  qty        DECIMAL(20,6) NOT NULL COMMENT '需求数量（含损耗）',
  issued_qty DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已领数量',
  remark     VARCHAR(255),
  creator    VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater    VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id  BIGINT        NOT NULL DEFAULT 1,
  company_id BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_order (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '工单材料需求行';

CREATE TABLE IF NOT EXISTS erp_mrp_material_req (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  no            VARCHAR(32)   NOT NULL COMMENT '单号 MRyyyyMMxxxx',
  work_order_id BIGINT        NOT NULL,
  warehouse_id  BIGINT        NOT NULL,
  total_qty     DECIMAL(20,6) NOT NULL DEFAULT 0,
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
  KEY idx_order (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '生产领料单';

CREATE TABLE IF NOT EXISTS erp_mrp_material_req_item (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  req_id            BIGINT        NOT NULL,
  work_order_item_id BIGINT       NOT NULL COMMENT '工单材料行',
  product_id        BIGINT        NOT NULL,
  qty               DECIMAL(20,6) NOT NULL COMMENT '领料数量',
  remark            VARCHAR(255),
  creator           VARCHAR(64)   NOT NULL DEFAULT '',
  create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater           VARCHAR(64)   NOT NULL DEFAULT '',
  update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted           BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id         BIGINT        NOT NULL DEFAULT 1,
  company_id        BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_req (req_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '领料明细';

-- ---------------------------------------------------------------------
-- 菜单：生产制造(7)
-- ---------------------------------------------------------------------
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (7,    '生产制造', '', 1, 7, 0, '/mrp', ''),
  (700,  'BOM 管理', '', 2, 1, 7, 'bom', 'mrp/bom/index'),
  (7001, 'BOM 查询', 'mrp:bom:query', 3, 1, 700, '', ''),
  (7002, 'BOM 新增', 'mrp:bom:create', 3, 2, 700, '', ''),
  (7003, 'BOM 修改', 'mrp:bom:update', 3, 3, 700, '', ''),
  (7004, 'BOM 删除', 'mrp:bom:delete', 3, 4, 700, '', ''),
  (701,  '生产工单', '', 2, 2, 7, 'work-order', 'mrp/work-order/index'),
  (7011, '工单查询', 'mrp:order:query', 3, 1, 701, '', ''),
  (7012, '工单新增', 'mrp:order:create', 3, 2, 701, '', ''),
  (7013, '工单删除', 'mrp:order:delete', 3, 3, 701, '', ''),
  (7014, '工单下达', 'mrp:order:audit', 3, 4, 701, '', ''),
  (7015, '工单作废', 'mrp:order:void', 3, 5, 701, '', ''),
  (7016, '完工入库', 'mrp:order:finish', 3, 6, 701, '', ''),
  (702,  '领料单', '', 2, 3, 7, 'material-req', 'mrp/material-req/index'),
  (7021, '领料查询', 'mrp:req:query', 3, 1, 702, '', ''),
  (7022, '领料新增', 'mrp:req:create', 3, 2, 702, '', ''),
  (7023, '领料删除', 'mrp:req:delete', 3, 3, 702, '', ''),
  (7024, '领料审核', 'mrp:req:audit', 3, 4, 702, '', ''),
  (7025, '领料作废', 'mrp:req:void', 3, 5, 702, '', ''),
  (703,  'MRP 试算', '', 2, 4, 7, 'calc', 'mrp/calc/index'),
  (7031, '试算查询', 'mrp:calc:query', 3, 1, 703, '', '');

-- ---------------------------------------------------------------------
-- 单号规则
-- ---------------------------------------------------------------------
INSERT INTO bas_serial_rule (biz_type, prefix, date_format, seq_length, reset_type, remark) VALUES
  ('mrp_bom',      'BOM', 'yyyyMM', 4, 'month', 'BOM 编号'),
  ('work_order',   'MO',  'yyyyMM', 4, 'month', '生产工单'),
  ('material_req', 'MR',  'yyyyMM', 4, 'month', '生产领料单');
