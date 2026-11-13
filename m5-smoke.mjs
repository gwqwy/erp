// M5 冒烟：多级审批流 / 红字冲销 / 批次 FIFO / 财务报表 / 请假 / 低代码表单 / 质检
const BASE = 'http://localhost:48080';

async function api(method, url, body, token) {
  const res = await fetch(BASE + url, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: body ? JSON.stringify(body) : undefined
  });
  const json = await res.json();
  if (json.code !== 0) throw new Error(`${url} -> code=${json.code} msg=${json.msg}`);
  return json.data;
}

const login = await api('POST', '/admin-api/system/auth/login', { username: 'admin', password: 'admin123' });
const t = login.accessToken;
console.log('1. 登录 OK (admin userId=' + login.userId + ')');

// ========== A. 多级审批流（采购订单 两级审批，admin 自己两级都当） ==========
// 幂等：已有配置就复用（V7 加了 (biz_type,level) 唯一约束）
const cfgPage = await api('GET', '/admin-api/bpm/config/page?pageNo=1&pageSize=50&bizType=pur_order', null, t);
if (!cfgPage.list.some(c => Number(c.level) === 1)) {
  await api('POST', `/admin-api/bpm/config/create?bizType=pur_order&level=1&approverUserId=${login.userId}`, null, t);
}
if (!cfgPage.list.some(c => Number(c.level) === 2)) {
  await api('POST', `/admin-api/bpm/config/create?bizType=pur_order&level=2&approverUserId=${login.userId}`, null, t);
}
console.log('2. 采购订单两级审批配置 OK');

const partners = await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=5&isSupplier=true', null, t);
const products = await api('GET', '/admin-api/bas/product/page?pageNo=1&pageSize=5', null, t);
const supplier = partners.list[0];
const product = products.list[0];
// 给产品开启批次管理（自动批次号 → FIFO 出库可拆分）
await api('PUT', '/admin-api/bas/product/update', {
  id: product.id, name: product.name, isBatch: true
}, t);
console.log('   产品已开启批次管理');
const poId = await api('POST', '/admin-api/pur/order/create', {
  supplierId: supplier.id, orderDate: '2026-11-07', remark: 'M5审批冒烟',
  items: [{ productId: product.id, unitId: product.unitId, qty: 5, price: 100 }]
}, t);
const poBefore = await api('GET', `/admin-api/pur/order/detail?id=${poId}`, null, t);
await api('POST', `/admin-api/pur/order/submit-approval?id=${poId}`, null, t);
const poAfter = await api('GET', `/admin-api/pur/order/detail?id=${poId}`, null, t);
console.log(`3. 提交审批 OK: 状态 ${poBefore.order.status} -> ${poAfter.order.status} (20=审批中)`);

const myTasks1 = await api('GET', '/admin-api/bpm/my-tasks', null, t);
const poIdStr = String(poId);
const level1 = myTasks1.find(x => x.bizType === 'pur_order' && String(x.bizId) === poIdStr && Number(x.level) === 1);
if (!level1) throw new Error('未找到本单第 1 级审批任务');
await api('POST', '/admin-api/bpm/approve', { taskId: level1.id, comment: '同意' }, t);
console.log('5. 第 1 级审批通过 → 流转第 2 级');
const myTasks2 = await api('GET', '/admin-api/bpm/my-tasks', null, t);
const level2 = myTasks2.find(x => x.bizType === 'pur_order' && String(x.bizId) === poIdStr && Number(x.level) === 2);
if (!level2) throw new Error('未找到本单第 2 级审批任务');
await api('POST', '/admin-api/bpm/approve', { taskId: level2.id, comment: '同意' }, t);
const poFinal = await api('GET', `/admin-api/pur/order/detail?id=${poId}`, null, t);
console.log(`6. 第 2 级审批通过 → 订单状态=${poFinal.order.status} (30=已审核，自动过账)`);

// ========== B. 批次 FIFO：分两批入库不同价，一笔出库验证拆分 ==========
const warehouses = await api('GET', '/admin-api/bas/warehouse/list', null, t);
const warehouse = warehouses[0];
// 批次A：5@100；批次B：5@200
await api('POST', '/admin-api/pur/inbound/create', {
  supplierId: supplier.id, warehouseId: warehouse.id, inboundDate: '2026-11-07',
  items: [{ productId: product.id, unitId: product.unitId, qty: 5, price: 100 }]
}, t);
const pis = await api('GET', '/admin-api/pur/inbound/page?pageNo=1&pageSize=5&status=10', null, t);
// 直接建两张草稿分别审核（同一产品无来源直入）
const piA = await api('POST', '/admin-api/pur/inbound/create', {
  supplierId: supplier.id, warehouseId: warehouse.id, inboundDate: '2026-11-07',
  items: [{ productId: product.id, unitId: product.unitId, qty: 5, price: 100 }]
}, t);
const piB = await api('POST', '/admin-api/pur/inbound/create', {
  supplierId: supplier.id, warehouseId: warehouse.id, inboundDate: '2026-11-07',
  items: [{ productId: product.id, unitId: product.unitId, qty: 5, price: 200 }]
}, t);
await api('POST', `/admin-api/pur/inbound/audit?id=${piA}`, null, t);
await api('POST', `/admin-api/pur/inbound/audit?id=${piB}`, null, t);
console.log('7. 批次A(5@100) + 批次B(5@200) 入库审核 OK');
const movesIn = await api('GET', '/admin-api/stk/move/page?pageNo=1&pageSize=10', null, t);
console.log('   流水批次列:', movesIn.list.filter(m => m.direction === 1).slice(0, 2).map(m => `batch=${m.batchNo}@${m.price}`).join(', '));

// 出库 N 件：N = 最老批次行库存 + 1，强制跨批次拆分（FIFO 验证）
const quantRows = (await api('GET', '/admin-api/stk/quant/page?pageNo=1&pageSize=50', null, t))
  .list.filter(q => String(q.productId) === String(product.id) && Number(q.qty) > 0);
if (quantRows.length < 2) throw new Error('❌ 预期产品有 >=2 个批次行，实际 ' + quantRows.length);
const outQty = Number(quantRows[0].qty) + 1;
console.log(`   出库量 = 最老行(${quantRows[0].batchNo || '无批次'}: ${quantRows[0].qty}) + 1 = ${outQty}`);
const customer = (await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=5&isCustomer=true', null, t)).list[0];
const soId = await api('POST', '/admin-api/sal/order/create', {
  customerId: customer.id, orderDate: '2026-11-07',
  items: [{ productId: product.id, unitId: product.unitId, qty: outQty, price: 300 }]
}, t);
await api('POST', `/admin-api/sal/order/audit?id=${soId}`, null, t);
const soItems = await api('GET', `/admin-api/sal/order/item-list?orderId=${soId}`, null, t);
const sdId = await api('POST', '/admin-api/sal/outbound/create', {
  customerId: customer.id, warehouseId: warehouse.id, orderId: soId,
  items: soItems.map(i => ({ orderItemId: i.id, productId: i.productId, unitId: i.unitId, qty: outQty, price: i.price }))
}, t);
await api('POST', `/admin-api/sal/outbound/audit?id=${sdId}`, null, t);
const sdIdStr = String(sdId);
const allOutMoves = await api('GET', '/admin-api/stk/move/page?pageNo=1&pageSize=50&direction=-1', null, t);
const fifoMoves = allOutMoves.list.filter(m => m.bizType === 'sale_out' && String(m.bizId) === sdIdStr);
console.log(`8. FIFO 出库拆分 OK: ${fifoMoves.map(m => `${m.qty}@${m.price}`).join(' + ')} （预期 -5@100 + -3@200，两笔拆分）`);
if (fifoMoves.length !== 2) throw new Error('❌ FIFO 未拆分成两笔');

// ========== C. 红字冲销：冲销上面这张出库单 ==========
await api('POST', `/admin-api/sal/outbound/reverse?id=${sdId}`, null, t);
const quantAfter = await api('GET', '/admin-api/stk/quant/page?pageNo=1&pageSize=50', null, t);
const pq = quantAfter.list.filter(q => String(q.productId) === String(product.id));
const totalQty = pq.reduce((s, q) => s + Number(q.qty), 0);
console.log(`9. 出库单红字冲销 OK: 货物回库，该产品总库存=${totalQty}`);
const sdAfter = await api('GET', `/admin-api/sal/outbound/detail?id=${sdId}`, null, t);
console.log(`   出库单状态=${sdAfter.outbound.status} (60=已冲销)`);

// ========== D. 财务报表 ==========
const balance = await api('GET', '/admin-api/fin/report/balance?period=2026-11', null, t);
console.log(`10. 科目余额表 OK: ${balance.length} 行`);
const profit = await api('GET', '/admin-api/fin/report/profit?period=2026-11', null, t);
console.log(`    利润简表: 收入=${profit.revenue} 成本=${profit.cost} 利润=${profit.profit}`);

// ========== E. 请假 + 低代码表单 + 质检 ==========
const empId = await api('POST', '/admin-api/hr/employee/create', { name: '李测试' }, t);
const lvId = await api('POST', `/admin-api/hr/leave/create?employeeId=${empId}&leaveType=年假&startDate=2026-11-10&endDate=2026-11-11&reason=M5冒烟`, null, t);
await api('POST', `/admin-api/hr/leave/audit?id=${lvId}`, null, t);
console.log('11. 请假创建+审核 OK');

const formId = await api('POST', `/admin-api/infra/form/def/create?name=设备报修登记&fieldsJson=${encodeURIComponent(JSON.stringify([
  { prop: 'device', label: '设备名', type: 'input', required: true },
  { prop: 'issue', label: '故障描述', type: 'textarea' }
]))}`, null, t);
await api('POST', `/admin-api/infra/form/data/create?formDefId=${formId}&dataJson=${encodeURIComponent(JSON.stringify({ device: 'CNC-01', issue: '主轴异响' }))}`, null, t);
const formData = await api('GET', `/admin-api/infra/form/data/page?formDefId=${formId}&pageNo=1&pageSize=5`, null, t);
console.log(`12. 低代码表单 OK: 定义+填报 ${formData.total} 条`);

await api('POST', `/admin-api/mrp/quality-check/create?no=QC001&checkType=iqc&productId=${product.id}&checkQty=10&qualifiedQty=9&result=1`, null, t);
console.log('13. 质检记录 OK');

console.log('\n✅ M5 冒烟测试全部通过');
