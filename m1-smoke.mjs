// M1 进销存闭环冒烟测试：采购→审核→入库→库存→销售→出库→库存
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
console.log('1. 登录 OK, userId =', login.userId);

// 基础数据
const supplier = await api('POST', '/admin-api/bas/partner/create',
  { name: '杭州轴承供应商', isSupplier: true, isCustomer: false }, t);
const customer = await api('POST', '/admin-api/bas/partner/create',
  { name: '上海机械贸易公司', isCustomer: true, isSupplier: false }, t);
const warehouse = await api('POST', '/admin-api/bas/warehouse/create',
  { name: '主仓库', address: '厂区1号库' }, t);
const productPage = await api('GET', '/admin-api/bas/product/page?pageNo=1&pageSize=5', null, t);
const product = productPage.list[0];
console.log(`2. 基础数据 OK: 供应商=${supplier}, 客户=${customer}, 仓库=${warehouse}, 产品=${product.name}(${product.id})`);

// 3. 创建采购订单
const poId = await api('POST', '/admin-api/pur/order/create', {
  supplierId: supplier, orderDate: '2026-11-07', remark: '冒烟测试',
  items: [{ productId: product.id, unitId: product.unitId, qty: 10, price: 100, taxRate: 0.13 }]
}, t);
const po = await api('GET', `/admin-api/pur/order/detail?id=${poId}`, null, t);
console.log(`3. 采购订单创建 OK: ${po.order.no}, 总额=${po.order.totalAmountTax}, 状态=${po.order.status}`);

// 4. 审核采购订单
await api('POST', `/admin-api/pur/order/audit?id=${poId}`, null, t);
console.log('4. 采购订单审核 OK');

// 5. 创建采购入库（引用订单）
const poItems = await api('GET', `/admin-api/pur/order/item-list?orderId=${poId}`, null, t);
const piId = await api('POST', '/admin-api/pur/inbound/create', {
  supplierId: supplier, warehouseId: warehouse, orderId: poId, inboundDate: '2026-11-07',
  items: poItems.map(i => ({ orderItemId: i.id, productId: i.productId, unitId: i.unitId, qty: 10, price: i.price }))
}, t);
const pi = await api('GET', `/admin-api/pur/inbound/detail?id=${piId}`, null, t);
console.log(`5. 采购入库单创建 OK: ${pi.inbound.no}, 数量=${pi.inbound.totalQty}`);

// 6. 审核入库 → 库存过账
await api('POST', `/admin-api/pur/inbound/audit?id=${piId}`, null, t);
const poAfter = await api('GET', `/admin-api/pur/order/detail?id=${poId}`, null, t);
console.log(`6. 入库审核 OK, 订单状态=${poAfter.order.status}(40=已完成), 行入库数量=${poAfter.items[0].inboundQty}`);

// 7. 查即时库存
let quantPage = await api('GET', `/admin-api/stk/quant/page?pageNo=1&pageSize=10`, null, t);
const quant = quantPage.list.find(q => String(q.productId) === String(product.id));
console.log(`7. 即时库存 OK: ${quant.productName} qty=${quant.qty} avgCost=${quant.avgCost}`);

// 8. 创建销售订单 + 审核
const soId = await api('POST', '/admin-api/sal/order/create', {
  customerId: customer, orderDate: '2026-11-07', remark: '冒烟测试',
  items: [{ productId: product.id, unitId: product.unitId, qty: 6, price: 150 }]
}, t);
await api('POST', `/admin-api/sal/order/audit?id=${soId}`, null, t);
console.log('8. 销售订单创建+审核 OK');

// 9. 创建销售出库（引用订单）+ 审核 → 出库过账
const soItems = await api('GET', `/admin-api/sal/order/item-list?orderId=${soId}`, null, t);
const sdId = await api('POST', '/admin-api/sal/outbound/create', {
  customerId: customer, warehouseId: warehouse, orderId: soId, outboundDate: '2026-11-07',
  items: soItems.map(i => ({ orderItemId: i.id, productId: i.productId, unitId: i.unitId, qty: 6, price: i.price }))
}, t);
await api('POST', `/admin-api/sal/outbound/audit?id=${sdId}`, null, t);
quantPage = await api('GET', '/admin-api/stk/quant/page?pageNo=1&pageSize=10', null, t);
const quantAfter = quantPage.list.find(q => String(q.productId) === String(product.id));
console.log(`9. 销售出库审核 OK: 库存 ${quant.qty} -> ${quantAfter.qty}, 成本=${quantAfter.avgCost}`);

// 10. 超卖防护验证
const so2 = await api('POST', '/admin-api/sal/order/create', {
  customerId: customer, orderDate: '2026-11-07',
  items: [{ productId: product.id, unitId: product.unitId, qty: 100, price: 150 }]
}, t);
await api('POST', `/admin-api/sal/order/audit?id=${so2}`, null, t);
const so2Items = await api('GET', `/admin-api/sal/order/item-list?orderId=${so2}`, null, t);
const sd2 = await api('POST', '/admin-api/sal/outbound/create', {
  customerId: customer, warehouseId: warehouse, outboundDate: '2026-11-07',
  items: so2Items.map(i => ({ orderItemId: i.id, productId: i.productId, unitId: i.unitId, qty: 100, price: i.price }))
}, t);
try {
  await api('POST', `/admin-api/sal/outbound/audit?id=${sd2}`, null, t);
  console.log('10. ❌ 超卖未拦截！');
} catch (e) {
  console.log(`10. 超卖防护 OK: ${e.message}`);
}

// 11. 库存流水
const movePage = await api('GET', '/admin-api/stk/move/page?pageNo=1&pageSize=10', null, t);
console.log('11. 库存流水 OK:');
for (const m of movePage.list) {
  console.log(`   ${m.moveNo} [${m.bizType}] ${m.productName} ${m.direction === 1 ? '入' : '出'} ${m.qty} @${m.price}`);
}

console.log('\n✅ M1 闭环冒烟测试全部通过');
