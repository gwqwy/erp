-- =====================================================================
-- 天工 ERP V6：M5 冲销状态 / 多级审批 / 批次追溯 / 质检 / 请假薪资 / 低代码表单
-- =====================================================================

-- 审批流配置与任务
CREATE TABLE IF NOT EXISTS bpm_approval_config (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  biz_type        VARCHAR(32)  NOT NULL COMMENT '业务类型',
  level           INT          NOT NULL COMMENT '审批级次，1 起始',
  approver_user_id BIGINT      NOT NULL COMMENT '该级次审批人',
  enabled         TINYINT      NOT NULL DEFAULT 1,
  creator         VARCHAR(64)  NOT NULL DEFAULT '',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater         VARCHAR(64)  NOT NULL DEFAULT '',
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id       BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_biz (biz_type, level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '审批流配置';

CREATE TABLE IF NOT EXISTS bpm_approval_task (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  biz_type         VARCHAR(32)  NOT NULL,
  biz_id           BIGINT       NOT NULL,
  biz_no           VARCHAR(32),
  level            INT          NOT NULL,
  approver_user_id BIGINT       NOT NULL,
  status           TINYINT      NOT NULL DEFAULT 1 COMMENT '1 待审批 2 通过 3 驳回 4 作废',
  comment          VARCHAR(500),
  creator          VARCHAR(64)  NOT NULL DEFAULT '',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater          VARCHAR(64)  NOT NULL DEFAULT '',
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id        BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_biz (biz_type, biz_id),
  KEY idx_approver (approver_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '审批任务实例';

-- 质检记录
CREATE TABLE IF NOT EXISTS erp_quality_check (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  no            VARCHAR(32)   NOT NULL COMMENT '质检单号',
  check_type    VARCHAR(8)    NOT NULL COMMENT 'iqc 来料检 / fqc 完工检',
  source_id     BIGINT                 COMMENT '来源单据 id',
  source_no     VARCHAR(32),
  product_id    BIGINT        NOT NULL,
  check_qty     DECIMAL(20,6) NOT NULL,
  qualified_qty DECIMAL(20,6) NOT NULL,
  inspector     VARCHAR(32),
  check_date    DATE,
  result        TINYINT       NOT NULL DEFAULT 1 COMMENT '1 合格 2 不合格 3 让步接收',
  remark        VARCHAR(500),
  creator       VARCHAR(64)   NOT NULL DEFAULT '',
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater       VARCHAR(64)   NOT NULL DEFAULT '',
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id     BIGINT        NOT NULL DEFAULT 1,
  company_id    BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_type (check_type),
  KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '质检记录';

-- 请假 / 薪资
CREATE TABLE IF NOT EXISTS hr_leave (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)  NOT NULL COMMENT '单号 LVyyyyMMxxxx',
  employee_id BIGINT       NOT NULL,
  leave_type  VARCHAR(16)  NOT NULL COMMENT '事假/年假/病假/调休',
  start_date  DATE         NOT NULL,
  end_date    DATE         NOT NULL,
  reason      VARCHAR(500),
  status      TINYINT      NOT NULL DEFAULT 10 COMMENT '10草稿 20审批中 30已通过 50已驳回 60已作废',
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_employee (employee_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '请假申请';

CREATE TABLE IF NOT EXISTS hr_salary (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  period       VARCHAR(7)    NOT NULL COMMENT '期间 yyyy-MM',
  employee_id  BIGINT        NOT NULL,
  gross_amount DECIMAL(20,6) NOT NULL COMMENT '应发',
  deduction    DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '扣款',
  net_amount   DECIMAL(20,6) NOT NULL COMMENT '实发',
  remark       VARCHAR(500),
  creator      VARCHAR(64)   NOT NULL DEFAULT '',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)   NOT NULL DEFAULT '',
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id    BIGINT        NOT NULL DEFAULT 1,
  company_id   BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_period (period, employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '月度薪资';

-- 低代码表单
CREATE TABLE IF NOT EXISTS infra_form_def (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL COMMENT '表单名称',
  fields_json TEXT         COMMENT '字段 schema JSON',
  status      TINYINT      NOT NULL DEFAULT 0,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '表单定义';

CREATE TABLE IF NOT EXISTS infra_form_data (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  form_def_id BIGINT       NOT NULL,
  data_json   TEXT         COMMENT '填报数据 JSON',
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_def (form_def_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '表单填报数据';

-- ---------------------------------------------------------------------
-- 菜单（id 段 15000+，避开历史迁移）
-- ---------------------------------------------------------------------
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15000, '审批中心', '', 1, 10, 0, '/bpm', ''),
  (15001, '我的待办', '', 2, 1, 15000, 'my-tasks', 'bpm/my-tasks/index'),
  (15002, '待办查看', '', 3, 1, 15001, '', ''),
  (15003, '审批操作', 'bpm:task:approve', 3, 2, 15001, '', ''),
  (15004, '审批配置', '', 2, 2, 15000, 'config', 'bpm/config/index'),
  (15005, '配置查询', 'bpm:config:query', 3, 1, 15004, '', ''),
  (15006, '配置新增', 'bpm:config:create', 3, 2, 15004, '', ''),
  (15007, '配置修改', 'bpm:config:update', 3, 3, 15004, '', ''),
  (15008, '配置删除', 'bpm:config:delete', 3, 4, 15004, '', ''),
  (15010, '报表中心', '', 1, 11, 0, '/report', ''),
  (15011, '科目余额表', '', 2, 1, 15010, 'balance', 'report/balance/index'),
  (15012, '余额表查询', 'fin:report:query', 3, 1, 15011, '', ''),
  (15013, '利润简表', '', 2, 2, 15010, 'profit', 'report/profit/index'),
  (15014, '利润表查询', 'fin:report:query', 3, 1, 15013, '', ''),
  (15020, '更多功能', '', 1, 12, 0, '/more', ''),
  (15021, '批次追溯', '', 2, 1, 15020, 'batch-trace', 'more/batch-trace/index'),
  (15022, '批次查询', 'stk:quant:query', 3, 1, 15021, '', ''),
  (15023, '质检记录', '', 2, 2, 15020, 'quality-check', 'more/quality-check/index'),
  (15024, '质检查询', 'mrp:quality:query', 3, 1, 15023, '', ''),
  (15025, '质检新增', 'mrp:quality:create', 3, 2, 15023, '', ''),
  (15026, '质检删除', 'mrp:quality:delete', 3, 3, 15023, '', ''),
  (15027, '动态表单', '', 2, 3, 15020, 'dynamic-form', 'more/dynamic-form/index'),
  (15028, '表单使用', 'infra:form:use', 3, 1, 15027, '', ''),
  (15029, '表单管理', 'infra:form:create', 3, 2, 15027, '', ''),
  (15030, '表单数据', '', 2, 4, 15020, 'form-data', 'more/form-data/index'),
  (15031, '数据查看', 'infra:form:use', 3, 1, 15030, '', '');

-- 审批/冲销按钮权限挂到既有单据菜单下
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15040, '订单提交审批', 'pur:order:submit', 3, 7, 300, '', ''),
  (15041, '入库冲销', 'pur:inbound:reverse', 3, 6, 301, '', ''),
  (15042, '订单提交审批', 'sal:order:submit', 3, 7, 400, '', ''),
  (15043, '出库冲销', 'sal:outbound:reverse', 3, 6, 401, '', ''),
  (15044, '收款冲销', 'fin:receipt:reverse', 3, 6, 602, '', ''),
  (15045, '付款冲销', 'fin:payment:reverse', 3, 6, 603, '', ''),
  (15046, '请假管理', '', 2, 2, 9, 'leave', 'hr/leave/index'),
  (15047, '请假查询', 'hr:leave:query', 3, 1, 15046, '', ''),
  (15048, '请假申请', 'hr:leave:create', 3, 2, 15046, '', ''),
  (15049, '请假审核', 'hr:leave:audit', 3, 3, 15046, '', ''),
  (15050, '请假删除', 'hr:leave:delete', 3, 4, 15046, '', ''),
  (15051, '薪资管理', '', 2, 3, 9, 'salary', 'hr/salary/index'),
  (15052, '薪资查询', 'hr:salary:query', 3, 1, 15051, '', ''),
  (15053, '薪资登记', 'hr:salary:create', 3, 2, 15051, '', ''),
  (15054, '薪资删除', 'hr:salary:delete', 3, 3, 15051, '', '');

-- 单号规则
INSERT INTO bas_serial_rule (biz_type, prefix, date_format, seq_length, reset_type, remark) VALUES
  ('finance_voucher_red', 'HZ', 'yyyyMM', 4, 'month', '红字冲销凭证'),
  ('hr_leave',            'LV', 'yyyyMM', 4, 'month', '请假申请');
