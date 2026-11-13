-- =====================================================================
-- 天工 ERP V1 初始化：平台表（system_*）+ 基础主数据（erp_* / bas_*）
-- 设计文档：docs/03-开发文档-数据库设计.md
-- 说明：admin 用户由应用启动时的 AdminUserInitializer 创建（运行时 BCrypt 加密）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 平台：组织与用户
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_dept (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门编号',
  name           VARCHAR(64)  NOT NULL                COMMENT '部门名称',
  parent_id      BIGINT       NOT NULL DEFAULT 0      COMMENT '父部门编号，0 为根',
  sort           INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
  leader_user_id BIGINT                                COMMENT '负责人',
  status         TINYINT      NOT NULL DEFAULT 0      COMMENT '0 启用 1 停用',
  creator        VARCHAR(64)  NOT NULL DEFAULT '',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater        VARCHAR(64)  NOT NULL DEFAULT '',
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id      BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '部门表';

CREATE TABLE IF NOT EXISTS system_users (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户编号',
  username    VARCHAR(32)  NOT NULL                COMMENT '用户名',
  password    VARCHAR(128) NOT NULL                COMMENT '密码（BCrypt）',
  nickname    VARCHAR(64)  NOT NULL                COMMENT '昵称',
  dept_id     BIGINT                                COMMENT '部门',
  status      TINYINT      NOT NULL DEFAULT 0      COMMENT '0 启用 1 停用',
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户表';

CREATE TABLE IF NOT EXISTS system_role (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL COMMENT '角色名称',
  code        VARCHAR(64)  NOT NULL COMMENT '角色编码',
  sort        INT          NOT NULL DEFAULT 0,
  status      TINYINT      NOT NULL DEFAULT 0,
  remark      VARCHAR(255)          DEFAULT NULL,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '角色表';

CREATE TABLE IF NOT EXISTS system_user_role (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  user_id     BIGINT   NOT NULL COMMENT '用户编号',
  role_id     BIGINT   NOT NULL COMMENT '角色编号',
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户角色关联';

CREATE TABLE IF NOT EXISTS system_menu (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL COMMENT '名称',
  permission  VARCHAR(64)           DEFAULT '' COMMENT '权限标识，如 sal:order:audit',
  type        TINYINT      NOT NULL COMMENT '1 目录 2 菜单 3 按钮',
  sort        INT          NOT NULL DEFAULT 0,
  parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单，0 为根',
  path        VARCHAR(128)          DEFAULT '' COMMENT '路由路径',
  component   VARCHAR(128)          DEFAULT '' COMMENT '前端组件路径',
  status      TINYINT      NOT NULL DEFAULT 0,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '菜单权限表';

CREATE TABLE IF NOT EXISTS system_role_menu (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  role_id     BIGINT   NOT NULL,
  menu_id     BIGINT   NOT NULL,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_role (role_id),
  KEY idx_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '角色菜单关联';

CREATE TABLE IF NOT EXISTS system_dict_type (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL,
  type        VARCHAR(64)  NOT NULL,
  status      TINYINT      NOT NULL DEFAULT 0,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_type (type, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '字典类型';

CREATE TABLE IF NOT EXISTS system_dict_data (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  dict_type   VARCHAR(64)  NOT NULL,
  label       VARCHAR(64)  NOT NULL,
  value       VARCHAR(64)  NOT NULL,
  sort        INT          NOT NULL DEFAULT 0,
  status      TINYINT      NOT NULL DEFAULT 0,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '字典数据';

-- ---------------------------------------------------------------------
-- ERP 基础主数据
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_product_category (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL COMMENT '分类名称',
  parent_id   BIGINT       NOT NULL DEFAULT 0,
  sort        INT          NOT NULL DEFAULT 0,
  status      TINYINT      NOT NULL DEFAULT 0,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1 COMMENT '所属公司',
  PRIMARY KEY (id),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '产品分类';

CREATE TABLE IF NOT EXISTS erp_unit (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(32)  NOT NULL COMMENT '单位名称',
  status      TINYINT      NOT NULL DEFAULT 0,
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '计量单位';

CREATE TABLE IF NOT EXISTS erp_product (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  no             VARCHAR(32)   NOT NULL COMMENT '产品编码',
  name           VARCHAR(128)  NOT NULL COMMENT '产品名称',
  category_id    BIGINT                 COMMENT '分类',
  unit_id        BIGINT                 COMMENT '默认计量单位',
  spec           VARCHAR(255)           COMMENT '规格型号',
  barcode        VARCHAR(64)            COMMENT '条码',
  purchase_price DECIMAL(20,6)          COMMENT '参考采购价',
  sale_price     DECIMAL(20,6)          COMMENT '参考销售价',
  tax_rate       DECIMAL(10,6)          COMMENT '默认税率',
  stock_min      DECIMAL(20,6)          COMMENT '安全库存下限',
  stock_max      DECIMAL(20,6)          COMMENT '库存上限',
  is_batch       BIT(1)       NOT NULL DEFAULT b'0' COMMENT '启用批次管理',
  is_serial      BIT(1)       NOT NULL DEFAULT b'0' COMMENT '启用序列号管理',
  status         TINYINT      NOT NULL DEFAULT 0,
  remark         VARCHAR(500),
  creator        VARCHAR(64)  NOT NULL DEFAULT '',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater        VARCHAR(64)  NOT NULL DEFAULT '',
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id      BIGINT       NOT NULL DEFAULT 1,
  company_id     BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_no (no, company_id),
  KEY idx_category (category_id),
  KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '产品';

CREATE TABLE IF NOT EXISTS erp_partner (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  no           VARCHAR(32)   NOT NULL COMMENT '编码',
  name         VARCHAR(128)  NOT NULL COMMENT '名称',
  short_name   VARCHAR(64),
  is_customer  BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是客户',
  is_supplier  BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是供应商',
  tax_no       VARCHAR(32)            COMMENT '税号',
  address      VARCHAR(255),
  contact      VARCHAR(64)            COMMENT '联系人',
  phone        VARCHAR(32),
  credit_limit DECIMAL(20,6)          COMMENT '信用额度',
  status       TINYINT       NOT NULL DEFAULT 0,
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
  KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '往来单位（客户+供应商合一）';

CREATE TABLE IF NOT EXISTS erp_warehouse (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  no               VARCHAR(32)           COMMENT '编码',
  name             VARCHAR(64)  NOT NULL COMMENT '仓库名称',
  address          VARCHAR(255),
  is_quality_check BIT(1)       NOT NULL DEFAULT b'0' COMMENT '收货是否质检',
  status           TINYINT      NOT NULL DEFAULT 0,
  remark           VARCHAR(500),
  creator          VARCHAR(64)  NOT NULL DEFAULT '',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater          VARCHAR(64)  NOT NULL DEFAULT '',
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id        BIGINT       NOT NULL DEFAULT 1,
  company_id       BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '仓库';

CREATE TABLE IF NOT EXISTS bas_serial_rule (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  biz_type    VARCHAR(64)  NOT NULL COMMENT '业务类型，如 sale_order',
  prefix      VARCHAR(16)  NOT NULL COMMENT '单号前缀',
  date_format VARCHAR(32)  NOT NULL DEFAULT 'yyyyMM' COMMENT '日期格式（resetType=none 时忽略）',
  seq_length  INT          NOT NULL DEFAULT 4 COMMENT '序列位数',
  reset_type  VARCHAR(16)  NOT NULL DEFAULT 'month' COMMENT 'none/year/month/day',
  remark      VARCHAR(255),
  creator     VARCHAR(64)  NOT NULL DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater     VARCHAR(64)  NOT NULL DEFAULT '',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     BIT(1)       NOT NULL DEFAULT b'0',
  tenant_id   BIGINT       NOT NULL DEFAULT 1,
  company_id  BIGINT       NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz (biz_type, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '单号规则';

-- ---------------------------------------------------------------------
-- 种子数据
-- ---------------------------------------------------------------------
INSERT INTO system_dept (id, name, parent_id, sort, status) VALUES
  (1, '天工集团', 0, 1, 0),
  (2, '综合管理部', 1, 1, 0),
  (3, '业务部', 1, 2, 0);

INSERT INTO system_role (id, name, code, sort, status, remark) VALUES
  (1, '超级管理员', 'super_admin', 1, 0, '内置角色：拥有全部权限');

INSERT INTO system_dict_type (id, name, type, status) VALUES
  (1, '通用状态', 'common_status', 0);
INSERT INTO system_dict_data (dict_type, label, value, sort) VALUES
  ('common_status', '启用', '0', 1),
  ('common_status', '停用', '1', 2);

-- 菜单：系统管理（1 目录 100-104 菜单 1xxx 按钮权限）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (1,    '系统管理', '', 1, 1, 0, '/system', ''),
  (100,  '用户管理', '', 2, 1, 1, 'user', 'system/user/index'),
  (1001, '用户查询', 'system:user:query', 3, 1, 100, '', ''),
  (1002, '用户新增', 'system:user:create', 3, 2, 100, '', ''),
  (1003, '用户修改', 'system:user:update', 3, 3, 100, '', ''),
  (1004, '用户删除', 'system:user:delete', 3, 4, 100, '', ''),
  (1005, '分配角色', 'system:user:assign-role', 3, 5, 100, '', ''),
  (101,  '角色管理', '', 2, 2, 1, 'role', 'system/role/index'),
  (1011, '角色查询', 'system:role:query', 3, 1, 101, '', ''),
  (1012, '角色新增', 'system:role:create', 3, 2, 101, '', ''),
  (1013, '角色修改', 'system:role:update', 3, 3, 101, '', ''),
  (1014, '角色删除', 'system:role:delete', 3, 4, 101, '', ''),
  (1015, '分配菜单', 'system:role:assign-menu', 3, 5, 101, '', ''),
  (102,  '菜单管理', '', 2, 3, 1, 'menu', 'system/menu/index'),
  (1021, '菜单查询', 'system:menu:query', 3, 1, 102, '', ''),
  (1022, '菜单新增', 'system:menu:create', 3, 2, 102, '', ''),
  (1023, '菜单修改', 'system:menu:update', 3, 3, 102, '', ''),
  (1024, '菜单删除', 'system:menu:delete', 3, 4, 102, '', ''),
  (103,  '部门管理', '', 2, 4, 1, 'dept', 'system/dept/index'),
  (1031, '部门查询', 'system:dept:query', 3, 1, 103, '', ''),
  (1032, '部门新增', 'system:dept:create', 3, 2, 103, '', ''),
  (1033, '部门修改', 'system:dept:update', 3, 3, 103, '', ''),
  (1034, '部门删除', 'system:dept:delete', 3, 4, 103, '', ''),
  (104,  '字典管理', '', 2, 5, 1, 'dict', 'system/dict/index'),
  (1041, '字典查询', 'system:dict:query', 3, 1, 104, '', ''),
  (1042, '字典新增', 'system:dict:create', 3, 2, 104, '', ''),
  (1043, '字典修改', 'system:dict:update', 3, 3, 104, '', ''),
  (1044, '字典删除', 'system:dict:delete', 3, 4, 104, '', '');

-- 菜单：ERP 基础数据（2 目录 2xx 菜单 2xxx 按钮）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (2,    'ERP 基础数据', '', 1, 2, 0, '/bas', ''),
  (200,  '产品管理', '', 2, 1, 2, 'product', 'bas/product/index'),
  (2001, '产品查询', 'bas:product:query', 3, 1, 200, '', ''),
  (2002, '产品新增', 'bas:product:create', 3, 2, 200, '', ''),
  (2003, '产品修改', 'bas:product:update', 3, 3, 200, '', ''),
  (2004, '产品删除', 'bas:product:delete', 3, 4, 200, '', ''),
  (201,  '产品分类', '', 2, 2, 2, 'product-category', 'bas/product-category/index'),
  (2011, '分类查询', 'bas:product-category:query', 3, 1, 201, '', ''),
  (2012, '分类新增', 'bas:product-category:create', 3, 2, 201, '', ''),
  (2013, '分类修改', 'bas:product-category:update', 3, 3, 201, '', ''),
  (2014, '分类删除', 'bas:product-category:delete', 3, 4, 201, '', ''),
  (202,  '计量单位', '', 2, 3, 2, 'unit', 'bas/unit/index'),
  (2021, '单位查询', 'bas:unit:query', 3, 1, 202, '', ''),
  (2022, '单位新增', 'bas:unit:create', 3, 2, 202, '', ''),
  (2023, '单位修改', 'bas:unit:update', 3, 3, 202, '', ''),
  (2024, '单位删除', 'bas:unit:delete', 3, 4, 202, '', ''),
  (203,  '往来单位', '', 2, 4, 2, 'partner', 'bas/partner/index'),
  (2031, '往来单位查询', 'bas:partner:query', 3, 1, 203, '', ''),
  (2032, '往来单位新增', 'bas:partner:create', 3, 2, 203, '', ''),
  (2033, '往来单位修改', 'bas:partner:update', 3, 3, 203, '', ''),
  (2034, '往来单位删除', 'bas:partner:delete', 3, 4, 203, '', ''),
  (204,  '仓库管理', '', 2, 5, 2, 'warehouse', 'bas/warehouse/index'),
  (2041, '仓库查询', 'bas:warehouse:query', 3, 1, 204, '', ''),
  (2042, '仓库新增', 'bas:warehouse:create', 3, 2, 204, '', ''),
  (2043, '仓库修改', 'bas:warehouse:update', 3, 3, 204, '', ''),
  (2044, '仓库删除', 'bas:warehouse:delete', 3, 4, 204, '', ''),
  (205,  '单号规则', '', 2, 6, 2, 'serial-rule', 'bas/serial-rule/index'),
  (2051, '规则查询', 'bas:serial-rule:query', 3, 1, 205, '', ''),
  (2052, '规则新增', 'bas:serial-rule:create', 3, 2, 205, '', ''),
  (2053, '规则修改', 'bas:serial-rule:update', 3, 3, 205, '', ''),
  (2054, '规则删除', 'bas:serial-rule:delete', 3, 4, 205, '', '');

-- 单号规则
INSERT INTO bas_serial_rule (biz_type, prefix, date_format, seq_length, reset_type, remark) VALUES
  ('erp_product',   'P',  'yyyyMM', 4, 'month', '产品编码'),
  ('erp_partner',   'BP', 'yyyyMM', 4, 'month', '往来单位编码'),
  ('sale_order',    'SO', 'yyyyMM', 4, 'month', '销售订单'),
  ('purchase_order','PO', 'yyyyMM', 4, 'month', '采购订单');
