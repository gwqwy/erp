# 开发文档 · API 规范与开发规范

> 配套：[02-总体架构](./02-开发文档-总体架构.md) · [03-数据库设计](./03-开发文档-数据库设计.md)

## 1. REST API 规范

### 1.1 路径与方法

```
GET    /admin-api/{module}/{resource}/page        # 分页查询
GET    /admin-api/{module}/{resource}/get?id=     # 详情
POST   /admin-api/{module}/{resource}/create      # 新增
PUT    /admin-api/{module}/{resource}/update      # 修改
DELETE /admin-api/{module}/{resource}/delete?id=  # 删除（逻辑删除）
POST   /admin-api/{module}/{resource}/audit       # 审核（业务单据）
POST   /admin-api/{module}/{resource}/void        # 作废/冲销
POST   /admin-api/{module}/{resource}/export      # 导出 Excel
POST   /admin-api/{module}/{resource}/import      # 导入
GET    /admin-api/{module}/{resource}/simple-list # 下拉选择（轻量，不分页）
```

- 模块段：`sal / pur / stk / fin / mrp / bas / crm / hr / rpt`；示例 `POST /admin-api/sal/order/audit`；
- 动词后置（`audit/void/submit`），不用 PUT 加路径动词的花式写法；
- 移动端 H5 走 `/app-api/`，复用同一 service 层。

### 1.2 统一返回体

```json
{ "code": 0, "data": { }, "msg": "" }
```

- `code = 0` 成功；非 0 为错误码（见 1.3），HTTP 状态码恒 200（网关层例外）；
- 分页请求：`{ pageNo: 1, pageSize: 20, ...筛选字段 }`，返回 `{ list, total }`；
- 排序：`sortBy=saleOrder:createTime&sortOrder=desc`，白名单字段，禁止前端传 SQL 片段。

### 1.3 错误码体系（沿用 yudao 段位规则）

```
错误码 = a * 1_000_000 + 模块段 * 10_000 + 序号
0        成功
400      客户端参数错误    401 未登录   403 无权限   404 不存在   423 被锁定   429 限流
500      服务端异常
1xx_xxxx  系统段(system/infra)
2xx_xxxx  sal / pur
3xx_xxxx  stk
4xx_xxxx  fin
5xx_xxxx  mrp
```

示例：`3-04-001 → 3040001` 库存不足。前端 toast 展示 `msg`，开发态附 `detail`。

### 1.4 幂等与并发

- 所有**改变单据状态**的接口（audit/void/posting）要求请求头 `Idempotency-Key`（前端生成 UUID，重试用同值）；
- 服务端 Redis `SET NX EX 600` 去重；单据维度再叠加分布式锁（单号维度），双保险；
- 前端提交前比对单据 `update_time`（乐观锁），冲突时提示"单据已被他人修改"。

## 2. 单号规则（服务端生成，详见 03 文档 §2.3）

`GET /admin-api/bas/serial-rule/preview?bizType=sale_order` 供页面预览；真正取号发生在 create 事务内，取号失败整体回滚。

## 3. 后端开发规范

### 3.1 分层与命名

| 层 | 规范 |
|----|------|
| Controller | 只做参数校验（`@Valid` + JSR303）与 DTO 转换，无业务逻辑；`@PreAuthorize("@ss.hasPermission('sal:order:query')")` |
| Service | 接口 + Impl；业务异常抛 `ServiceException(ERR_CODE, msg)`；跨模块调用只注入对方 `XxxApi` |
| DAL | DO 与表一一对应；查询用 MyBatis Plus LambdaQueryWrapper，复杂 SQL 允许 XML 但需走 `select` 前缀 mapper 隔离 |
| Convert | MapStruct，禁止手工 set 大段字段 |
| DTO | 请求 `XxxCreateReqVO / XxxPageReqVO`，响应 `XxxRespVO`；禁止 DO 直接出 Controller |

### 3.2 必须遵守的横切清单

- 新业务表：DO 继承 `BaseDO`（自动填充审计字段）；业务单据表 DO 额外带 `company_id`；
- 单据状态流转必须走 `OrderStateChecker`，禁止 service 里裸写 `if (status == 30)` 后直接更新；
- 写库存只准调 `StockPostingService.submit(MoveCommand)`；写应收应付只准调 `ReceivableApi/PayableApi`；
- 金额计算用 `BigDecimal`，`RoundingMode.HALF_UP`，精度常量集中定义（禁止 `double`/魔法数）；
- 事务边界：审核+过账+回写 在一个 `@Transactional`（REPEATABLE_READ），模板过账失败按架构文档 §4.3 降级为挂起；
- 所有单据审核成功后必须发布 Chatter system_trace 事件（组件自动，勿手写 SQL）。

### 3.3 代码生成器

`infra` 模块的代码生成器（yudao 二开）按表生成 Controller/Service/Mapper/VO/前端页面骨架；使用顺序：**先在 03 文档确认表设计 → 建表 → 导入生成器 → 选"单据"模板（自动带状态机/审核接口/Chatter）→ 微调**。生成物中状态机与过账钩子是模板增强部分，勿手工替换。

## 4. 前端开发规范

### 4.1 目录与组件

- 页面一律 `views/{module}/{biz}/index.vue`（列表）+ `XxxForm.vue`（表单弹窗/抽屉）+ `detail.vue`（详情+Chatter）；
- 列表页禁止手写 el-table 分页逻辑，统一用 `<BizTable :schema :api>`；业务选择器优先用现成组件（PartnerSelect/ProductSelect/WarehouseSelect）；
- API 封装：`api/{module}/{biz}.ts` 导出类型 + 方法；类型从 OpenAPI 生成（`openapi-typescript`），禁止手抄后端字段。

### 4.2 状态与权限

- 权限串与后端一致：`v-hasPermi="['sal:order:audit']"`；
- 单据页按 `status` 用 `useOrderStatus` hook 控制按钮显隐与颜色（草稿蓝/待审橙/已审绿/完成灰/作废红）；
- 表单内金额展示统一 `AmountInput`（千分位+精度），打印/详情用 `AmountText`。

## 5. Git 与协作规范

```
main            # 可发布
└── develop     # 集成分支
    ├── feature/1-进销存骨架        # 与里程碑任务号对应
    ├── fix/sal-3012041-负库存      # bug 单号
    └── release/v0.3
```

- 提交信息：`feat(sal): 销售出库审核联动库存过账 (#102)`，类型限 feat/fix/docs/refactor/test/chore；
- Code Review 红线（见 02 文档 §2）：跨模块直连 DAO、绕过过账服务、裸写状态号，一律打回；
- 敏感信息（数据库密码等）走环境变量，禁止入库。

## 6. 测试策略

| 层 | 要求 |
|----|------|
| 单元测试 | 金额计算、状态机、单号规则、MRP 展开算法 100% 覆盖；JUnit5 + Mockito |
| 集成测试 | 库存过账（含并发扣减 100 线程压测不超卖）、核销逻辑必须集成测试；Testcontainers 起 MySQL |
| 业务验收 | 每期上线前按《业务场景清单》跑全链路（建单→审→过账→报表对数） |

## 7. 环境与部署

```yaml
# docker-compose.yml 骨架
services:
  mysql:      { image: mysql:8.0,   volumes: [./data/mysql:/var/lib/mysql] }
  redis:      { image: redis:7 }
  minio:      { image: minio/minio }
  server:     { image: tiangong/server:${TAG}, depends_on: [mysql, redis] }
  nginx:      { image: nginx, ports: ["80:80"] }   # 托管前端静态资源 + 反代 /admin-api
```

- 发布流程：`develop → release → main` 打 tag → CI 构建 Docker 镜像 → 单机 compose up -d（企业自用规模无需 K8s）；
- 数据库变更：Flyway 管理（`V1__init.sql`、`V2__...`），禁止手工改库结构；
- 回滚：镜像回退 + Flyway 不做 down 脚本（只进不退，回滚用兼容性设计）。
