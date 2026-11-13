import fs from 'node:fs';
const p = 'E:/文件/编程文件/erp/tiangong-erp/README.md';
let s = fs.readFileSync(p, 'utf8');
const start = s.indexOf('## 当前状态');
const end = s.indexOf('## 快速启动');
const newBlock = `## 当前状态：V10 全模块丰富完成

- ✅ M0~M5：底座 + 进销存 + 财务一体化 + 生产制造 + CRM/HR/驾驶舱 + 审批/冲销/批次（详见 docs/ 与 git 历史）
- ✅ **V10 全模块丰富**：
  - **库存**：调拨（双仓过账、成本随迁）、盘点（账面快照→实盘→盈亏自动过账+凭证）、其他出入库（赠品/报废/领用）、**库存预警页**（安全上下限）
  - **销售**：销售退货（走引擎+冲减收入凭证）+ **客户信用额度控制**（出库校验"已占用应收+本单"不超额度）
  - **采购**：采购退货（走引擎+冲减应付凭证）
  - **CRM**：赢单商机一键转销售订单
  - **财务**：费用登记（审核自动凭证+账户扣款）、**应收账龄**（逾期天数）
  - **system**：操作日志（AOP 自动记录全部写操作：人/接口/参数/耗时/成败）
  - **infra**：公告通知（发布/撤回）
  - 前端新增 10 页：库存调拨/盘点/预警/其他出入库、销售退货、采购退货、费用登记、应收账龄、公告通知、操作日志
  - 顺带修复：前端路由显式组件映射（新页面免重启）；Redis 序列跳号基线提至 9000

`;
fs.writeFileSync(p, s.slice(0, start) + newBlock + s.slice(end), 'utf8');
console.log('README updated:', fs.readFileSync(p, 'utf8').includes('V10 全模块丰富完成'));
