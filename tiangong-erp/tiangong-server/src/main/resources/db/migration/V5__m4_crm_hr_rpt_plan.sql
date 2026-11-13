-- =====================================================================
-- 天工 ERP V5：M4 CRM / 人事 / MRP 计划落库
-- =====================================================================

CREATE TABLE IF NOT EXISTS crm_lead (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(128) NOT NULL COMMENT '线索名称',
  source      VARCHAR(32)           COMMENT '来源：电话/网络/转介绍/展会',
  contact     VARCHAR(64)           COMMENT '联系人',
  phone       VARCHAR(32),
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 待跟进 2 已转商机 3 无效',
  owner_user_id BIGINT              COMMENT '负责人',
  remark      VARCHAR(500),
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '销售线索';

CREATE TABLE IF NOT EXISTS crm_business (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  name           VARCHAR(128)  NOT NULL COMMENT '商机名称',
  partner_id     BIGINT        NOT NULL COMMENT '客户 erp_partner',
  amount         DECIMAL(20,6)           COMMENT '预计成交金额',
  stage          TINYINT       NOT NULL DEFAULT 1 COMMENT '1初步接触 2需求确认 3方案报价 4商务谈判 5赢单 6输单',
  expect_date    DATE                    COMMENT '预计成交日',
  source_lead_id BIGINT                  COMMENT '来源线索，可空',
  owner_user_id  BIGINT,
  remark         VARCHAR(500),
  creator        VARCHAR(64)   NOT NULL DEFAULT '',
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater        VARCHAR(64)   NOT NULL DEFAULT '',
  update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id      BIGINT        NOT NULL DEFAULT 1,
  company_id     BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_stage (stage),
  KEY idx_partner (partner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '商机（销售漏斗）';

CREATE TABLE IF NOT EXISTS erp_mrp_plan (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  no                VARCHAR(32)  NOT NULL COMMENT '计划号 PLyyyyMMxxxx',
  warehouse_id      BIGINT       NOT NULL,
  demands_json      TEXT         COMMENT '需求入参 JSON',
  result_json       TEXT         COMMENT '结果 JSON：叶子净需求行',
  purchase_order_id BIGINT                COMMENT '转采购生成的订单 id',
  status            TINYINT      NOT NULL DEFAULT 1 COMMENT '1 已保存 2 已转采购',
  remark            VARCHAR(500),
  creator           VARCHAR(64)  NOT NULL DEFAULT '',
  create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater           VARCHAR(64)  NOT NULL DEFAULT '',
  update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted           BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id         BIGINT       NOT NULL DEFAULT 1,
  company_id        BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'MRP 计划';

CREATE TABLE IF NOT EXISTS hr_employee (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)           COMMENT '工号',
  name        VARCHAR(64)  NOT NULL COMMENT '姓名',
  dept_id     BIGINT                COMMENT '部门 system_dept',
  post        VARCHAR(64)           COMMENT '岗位',
  phone       VARCHAR(32),
  email       VARCHAR(64),
  entry_date  DATE                  COMMENT '入职日期',
  leave_date  DATE                  COMMENT '离职日期',
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 在职 1 离职',
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_dept (dept_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '员工档案';

-- ---------------------------------------------------------------------
-- 菜单：CRM(8) / 人事(9) / 生产制造目录下追加 MRP 计划
-- ---------------------------------------------------------------------
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (8,    '客户关系', '', 1, 8, 0, '/crm', ''),
  (800,  '销售线索', '', 2, 1, 8, 'lead', 'crm/lead/index'),
  (8001, '线索查询', 'crm:lead:query', 3, 1, 800, '', ''),
  (8002, '线索新增', 'crm:lead:create', 3, 2, 800, '', ''),
  (8003, '线索修改', 'crm:lead:update', 3, 3, 800, '', ''),
  (8004, '线索删除', 'crm:lead:delete', 3, 4, 800, '', ''),
  (8005, '转商机', 'crm:lead:convert', 3, 5, 800, '', ''),
  (801,  '商机管理', '', 2, 2, 8, 'business', 'crm/business/index'),
  (8011, '商机查询', 'crm:business:query', 3, 1, 801, '', ''),
  (8012, '商机关联客户', 'crm:business:create', 3, 2, 801, '', ''),
  (8013, '商机阶段推进', 'crm:business:update', 3, 3, 801, '', ''),
  (8014, '商机删除', 'crm:business:delete', 3, 4, 801, '', ''),
  (9,    '人事管理', '', 1, 9, 0, '/hr', ''),
  (900,  '员工档案', '', 2, 1, 9, 'employee', 'hr/employee/index'),
  (9001, '员工查询', 'hr:employee:query', 3, 1, 900, '', ''),
  (9002, '员工新增', 'hr:employee:create', 3, 2, 900, '', ''),
  (9003, '员工修改', 'hr:employee:update', 3, 3, 900, '', ''),
  (9004, '员工删除', 'hr:employee:delete', 3, 4, 900, '', ''),
  (704,  'MRP 计划', '', 2, 5, 7, 'plan', 'mrp/plan/index'),
  (7041, '计划查询', 'mrp:plan:query', 3, 1, 704, '', ''),
  (7042, '计划保存', 'mrp:plan:create', 3, 2, 704, '', ''),
  (7043, '计划转采购', 'mrp:plan:convert', 3, 3, 704, '', '');

-- ---------------------------------------------------------------------
-- 单号规则
-- ---------------------------------------------------------------------
INSERT INTO bas_serial_rule (biz_type, prefix, date_format, seq_length, reset_type, remark) VALUES
  ('mrp_plan', 'PL', 'yyyyMM', 4, 'month', 'MRP 计划');
