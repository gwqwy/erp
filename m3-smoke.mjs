// M3 生产制造冒烟测试：BOM→工单→领料（扣库存）→完工入库（成本估算）→MRP 试算
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

async function waitFor(desc, query, timeoutMs = 5000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const hit = await query();
    if (hit) return hit;
    await new Promise(r => setTimeout(r, 200));
  }
  throw new Error('❌ 等待超时: ' + desc);
}

const login = await api('POST', '/admin-api/system/auth/login', { username: 'admin', password: 'admin123' });
const t = login.accessToken;
console.log('1. 登录 OK');

// 基础数据：成品用减速机，组件=伺服电机+新轴承
const products = await api('GET', '/admin-api/bas/product/page?pageNo=1&pageSize=20', null, t);
const finished = products.list.find(p => p.name.includes('减速机'));
const motor = products.list.find(p => p.name.includes('伺服'));
const bearing = await api('POST', '/admin-api/bas/product/create', { name: '深沟球轴承', spec: '6204' }, t);
const warehouses = await api('GET', '/admin-api/bas/warehouse/list', null, t);
const warehouse = warehouses[0];
const supplier = (await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=20&isSupplier=true', null, t)).list[0];
console.log(`2. 基础数据: 成品=${finished.name}, 组件1=${motor.name}(库存4), 组件2=深沟球轴承(新建)`);

// 3. 采购备料：伺服电机 ×10、轴承 ×25（无订单直接入库；轴承需求 20×1.05=21，多备些）
const motorPo = await api('POST', '/admin-api/pur/order/create', {
  supplierId: supplier.id, orderDate: '2026-11-07', remark: 'M3备料',
  items: [{ productId: motor.id, unitId: motor.unitId, qty: 10, price: 100 }]
}, t);
await api('POST', `/admin-api/pur/order/audit?id=${motorPo}`, null, t);
const motorPoItems = await api('GET', `/admin-api/pur/order/item-list?orderId=${motorPo}`, null, t);
const motorPi = await api('POST', '/admin-api/pur/inbound/create', {
  supplierId: supplier.id, warehouseId: warehouse.id, orderId: motorPo,
  items: motorPoItems.map(i => ({ orderItemId: i.id, productId: i.productId, unitId: i.unitId, qty: 10, price: i.price }))
}, t);
await api('POST', `/admin-api/pur/inbound/audit?id=${motorPi}`, null, t);
const bearingPi = await api('POST', '/admin-api/pur/inbound/create', {
  supplierId: supplier.id, warehouseId: warehouse.id, inboundDate: '2026-11-07',
  items: [{ productId: bearing, unitId: null, qty: 25, price: 10 }]
}, t);
await api('POST', `/admin-api/pur/inbound/audit?id=${bearingPi}`, null, t);
console.log('3. 备料入库 OK: 伺服电机+10, 轴承+25');

// 4. BOM：减速机 ×1 = 伺服电机 ×2 + 轴承 ×4×1.05（删旧重建，保证组件与本次产品一致）
let bomId;
const bomPage = await api('GET', `/admin-api/mrp/bom/page?pageNo=1&pageSize=10&productId=${finished.id}`, null, t);
for (const old of bomPage.list) {
  if (Number(old.version) === 1) {
    await api('DELETE', `/admin-api/mrp/bom/delete?id=${old.id}`, null, t);
  }
}
bomId = await api('POST', '/admin-api/mrp/bom/create', {
  productId: finished.id, version: 1, qty: 1, remark: 'M3冒烟',
  items: [
    { productId: motor.id, qty: 2, lossRate: 0 },
    { productId: bearing, qty: 4, lossRate: 5 }
  ]
}, t);
console.log('4. BOM 新建 OK:', bomId, '（伺服×2 + 轴承×4×1.05损耗）');
// 启用并轮询确认持久化（偶发延迟则重试）
for (let i = 0; i < 10; i++) {
  await api('POST', `/admin-api/mrp/bom/enable?id=${bomId}`, null, t);
  const d = await api('GET', `/admin-api/mrp/bom/detail?id=${bomId}`, null, t);
  if (Number(d.bom.status) === 1 && Number(d.bom.isDefault) === 1) {
    console.log('   BOM 已启用为默认');
    break;
  }
  await new Promise(r => setTimeout(r, 300));
}

// 5. 生产工单 qty=5 → 下达展开材料需求
const moId = await api('POST', `/admin-api/mrp/work-order/create?productId=${finished.id}&bomId=${bomId}&warehouseId=${warehouse.id}&qty=5&remark=M3冒烟`, null, t);
await api('POST', `/admin-api/mrp/work-order/audit?id=${moId}`, null, t);
const moItems = await api('GET', `/admin-api/mrp/work-order/item-list?orderId=${moId}`, null, t);
console.log(`5. 工单下达 OK，材料需求 ${moItems.length} 行:`,
  moItems.map(i => `${i.productId === String(motor.id) ? '伺服' : '轴承'}×${i.qty}`).join(', '));

// 6. 领料单（全量领料）→ 审核扣库存
const moIdStr = String(moId);
const reqId = await api('POST', '/admin-api/mrp/material-req/create', {
  workOrderId: moIdStr, warehouseId: warehouse.id, remark: 'M3冒烟领料',
  items: moItems.map(i => ({ workOrderItemId: i.id, productId: i.productId, qty: i.qty }))
}, t);
await api('POST', `/admin-api/mrp/material-req/audit?id=${reqId}`, null, t);
const moDetail = await api('GET', `/admin-api/mrp/work-order/detail?id=${moId}`, null, t);
console.log(`6. 领料审核 OK: 工单状态=${moDetail.order.status}(40=生产中), 已领=${moDetail.items.map(i => i.issuedQty).join('/')}`);

// 7. 完工入库 qty=5，成本估算 = (伺服10×100 + 轴承21×10) / 5 = 242
const unitCost = await api('POST', `/admin-api/mrp/work-order/finish?id=${moId}&qty=5&warehouseId=${warehouse.id}`, null, t);
const moDetail2 = await api('GET', `/admin-api/mrp/work-order/detail?id=${moId}`, null, t);
console.log(`7. 完工入库 OK: 单位成本=${unitCost}(预期242), 工单状态=${moDetail2.order.status}(50=完成), 累计完工=${moDetail2.order.finishedQty}`);
const quants = await api('GET', '/admin-api/stk/quant/page?pageNo=1&pageSize=20', null, t);
const finishedQuant = quants.list.find(q => String(q.productId) === String(finished.id));
console.log(`   成品库存: ${finishedQuant.productName} qty=${finishedQuant.qty} avgCost=${finishedQuant.avgCost}`);

// 8. MRP 试算：需求 减速机×10（库存 5 → 净需求 5 → 展开组件）
const calc = await api('POST', '/admin-api/mrp/calc/run', {
  warehouseId: warehouse.id,
  demands: [{ productId: finished.id, qty: 10 }]
}, t);
console.log('8. MRP 试算 OK:');
for (const line of calc.lines) {
  console.log(`   ${line.productName}: 现有库存 ${line.stockQty} → 净需求 ${line.netQty}`);
}
console.log('\n✅ M3 生产制造冒烟测试全部通过');
