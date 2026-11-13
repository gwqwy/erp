-- =====================================================================
-- V9：人事深化 —— 考勤 / 加班 / 出差 / 离职 / 奖惩 / 合同 / 员工基础工资
-- =====================================================================

ALTER TABLE hr_employee ADD COLUMN base_salary DECIMAL(20,6) NULL COMMENT '月基本工资' AFTER post;

-- 考勤（每日一条：employee + date 唯一）
CREATE TABLE IF NOT EXISTS hr_attendance (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  employee_id  BIGINT      NOT NULL,
  att_date     DATE        NOT NULL,
  check_in     TIME        NULL COMMENT '上班打卡',
  check_out    TIME        NULL COMMENT '下班打卡',
  status       TINYINT     NOT NULL DEFAULT 0 COMMENT '0 正常 1 迟到 2 早退 3 缺勤 4 事假 5 年假',
  remark       VARCHAR(255),
  creator      VARCHAR(64)  NOT NULL DEFAULT '',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater      VARCHAR(64)  NOT NULL DEFAULT '',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id    BIGINT       NOT NULL DEFAULT 1,
  company_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_emp_date (employee_id, att_date, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '考勤记录';

-- 加班申请（审批流）
CREATE TABLE IF NOT EXISTS hr_overtime (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)  NOT NULL COMMENT 'OTyyyyMMxxxx',
  employee_id BIGINT       NOT NULL,
  ot_date     DATE         NOT NULL,
  hours       DECIMAL(6,2) NOT NULL COMMENT '加班时数',
  reason      VARCHAR(500),
  status      TINYINT      NOT NULL DEFAULT 10 COMMENT '10草稿 20审批中 30已通过 50已驳回',
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_emp (employee_id, ot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '加班申请';

-- 出差申请（审批流）
CREATE TABLE IF NOT EXISTS hr_trip (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)  NOT NULL COMMENT 'BTyyyyMMxxxx',
  employee_id BIGINT       NOT NULL,
  start_date  DATE         NOT NULL,
  end_date    DATE         NOT NULL,
  destination VARCHAR(128) NOT NULL,
  reason      VARCHAR(500),
  status      TINYINT      NOT NULL DEFAULT 10,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '出差申请';

-- 离职申请（审批流，通过后自动改员工离职）
CREATE TABLE IF NOT EXISTS hr_resignation (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  no              VARCHAR(32)  NOT NULL COMMENT 'RZyyyyMMxxxx',
  employee_id     BIGINT       NOT NULL,
  apply_date      DATE         NOT NULL,
  plan_leave_date DATE         NOT NULL,
  reason          VARCHAR(500),
  status          TINYINT      NOT NULL DEFAULT 10,
  creator         VARCHAR(64)  NOT NULL DEFAULT '',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater         VARCHAR(64)  NOT NULL DEFAULT '',
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id       BIGINT       NOT NULL DEFAULT 1,
  company_id      BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '离职申请';

-- 奖惩记录
CREATE TABLE IF NOT EXISTS hr_reward_penalty (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)   NOT NULL COMMENT 'RPyyyyMMxxxx',
  employee_id BIGINT        NOT NULL,
  type        VARCHAR(8)    NOT NULL COMMENT 'reward 奖励 / penalty 处罚',
  amount      DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '金额（奖励正/处罚正数，算薪时取符号）',
  rp_date     DATE          NOT NULL,
  reason      VARCHAR(500),
  creator     VARCHAR(64)   NOT NULL DEFAULT '',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)   NOT NULL DEFAULT '',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)        NOT NULL DEFAULT b'0',
  tenant_id   BIGINT        NOT NULL DEFAULT 1,
  company_id  BIGINT        NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_emp (employee_id, rp_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '奖惩记录';

-- 劳动合同
CREATE TABLE IF NOT EXISTS hr_contract (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32)  NOT NULL COMMENT 'CTyyyyMMxxxx',
  employee_id BIGINT       NOT NULL,
  start_date  DATE         NOT NULL,
  end_date    DATE         NOT NULL,
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 履行中 1 已到期 2 已解除',
  remark      VARCHAR(500),
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_emp (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '劳动合同';

-- ---------------------------------------------------------------------
-- 菜单（id 段 15060+）
-- ---------------------------------------------------------------------
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15060, '考勤记录', '', 2, 4, 9, 'attendance', 'hr/attendance/index'),
  (15061, '考勤查询', 'hr:attendance:query', 3, 1, 15060, '', ''),
  (15062, '考勤打卡', 'hr:attendance:clock', 3, 2, 15060, '', ''),
  (15063, '考勤补录', 'hr:attendance:create', 3, 3, 15060, '', ''),
  (15064, '考勤统计', '', 2, 5, 9, 'attendance-summary', 'hr/attendance-summary/index'),
  (15065, '统计查看', 'hr:attendance:query', 3, 1, 15064, '', ''),
  (15066, '加班申请', '', 2, 6, 9, 'overtime', 'hr/overtime/index'),
  (15067, '加班查询', 'hr:overtime:query', 3, 1, 15066, '', ''),
  (15068, '加班申请', 'hr:overtime:create', 3, 2, 15066, '', ''),
  (15069, '加班审批', 'hr:overtime:audit', 3, 3, 15066, '', ''),
  (15070, '出差申请', '', 2, 7, 9, 'trip', 'hr/trip/index'),
  (15071, '出差查询', 'hr:trip:query', 3, 1, 15070, '', ''),
  (15072, '出差申请', 'hr:trip:create', 3, 2, 15070, '', ''),
  (15073, '出差审批', 'hr:trip:audit', 3, 3, 15070, '', ''),
  (15074, '离职申请', '', 2, 8, 9, 'resignation', 'hr/resignation/index'),
  (15075, '离职查询', 'hr:resign:query', 3, 1, 15074, '', ''),
  (15076, '离职申请', 'hr:resign:create', 3, 2, 15074, '', ''),
  (15077, '离职审批', 'hr:resign:audit', 3, 3, 15074, '', ''),
  (15078, '奖惩记录', '', 2, 9, 9, 'reward-penalty', 'hr/reward-penalty/index'),
  (15079, '奖惩查询', 'hr:reward:query', 3, 1, 15078, '', ''),
  (15080, '奖惩登记', 'hr:reward:create', 3, 2, 15078, '', ''),
  (15081, '奖惩删除', 'hr:reward:delete', 3, 3, 15078, '', ''),
  (15082, '合同管理', '', 2, 10, 9, 'contract', 'hr/contract/index'),
  (15083, '合同查询', 'hr:contract:query', 3, 1, 15082, '', ''),
  (15084, '合同登记', 'hr:contract:create', 3, 2, 15082, '', ''),
  (15085, '合同删除', 'hr:contract:delete', 3, 3, 15082, '', ''),
  (15086, '工资核算', '', 2, 11, 9, 'payroll', 'hr/payroll/index'),
  (15087, '算薪查看', 'hr:payroll:query', 3, 1, 15086, '', ''),
  (15088, '算薪生成', 'hr:payroll:generate', 3, 2, 15086, '', '');

-- 单号规则
INSERT INTO bas_serial_rule (biz_type, prefix, date_format, seq_length, reset_type, remark) VALUES
  ('hr_overtime',      'OT', 'yyyyMM', 4, 'month', '加班申请'),
  ('hr_trip',          'BT', 'yyyyMM', 4, 'month', '出差申请'),
  ('hr_resignation',   'RZ', 'yyyyMM', 4, 'month', '离职申请'),
  ('hr_reward_penalty','RP', 'yyyyMM', 4, 'month', '奖惩单'),
  ('hr_contract',      'CT', 'yyyyMM', 4, 'month', '劳动合同');
