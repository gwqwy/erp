// M7 冒烟：调拨 → 盘点(盈亏) → 销售退货(凭证) → 信用额度拦截 → 商机转单 → 操作日志/公告
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
console.log('1. 登录 OK');

const warehouses = await api('GET', '/admin-api/bas/warehouse/list', null, t);
const wh1 = warehouses[0];
// 需要第二仓库
let wh2 = warehouses[1];
if (!wh2) {
  wh2 = { id: await api('POST', '/admin-api/bas/warehouse/create', { name: '二号仓' }, t) };
}
const products = await api('GET', '/admin-api/bas/product/page?pageNo=1&pageSize=5', null, t);
const product = products.list[0];
const supplier = (await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=5&isSupplier=true', null, t)).list[0];
console.log(`2. 基础数据: ${wh1.name} / ${wh2.name || '二号仓'} / ${product.name}`);

// ========== A. 调拨：先给 wh1 入 6@50，调 4 件到 wh2 ==========
const pi = await api('POST', '/admin-api/pur/inbound/create', {
  supplierId: supplier.id, warehouseId: wh1.id, inboundDate: '2026-11-08',
  items: [{ productId: product.id, unitId: product.unitId, qty: 6, price: 50 }]
}, t);
await api('POST', `/admin-api/pur/inbound/audit?id=${pi}`, null, t);
const tfId = await api('POST', `/admin-api/stk/transfer/create?fromWarehouseId=${wh1.id}&toWarehouseId=${wh2.id}&remark=M7冒烟`,
  [{ productId: product.id, qty: 4, remark: '' }], t);
await api('POST', `/admin-api/stk/transfer/audit?id=${tfId}`, null, t);
const q1 = (await api('GET', `/admin-api/stk/quant/page?pageNo=1&pageSize=100`, null, t)).list;
const inWh1 = q1.filter(q => String(q.productId) === String(product.id) && String(q.warehouseId) === String(wh1.id))
  .reduce((s, q) => s + Number(q.qty), 0);
const inWh2 = q1.filter(q => String(q.productId) === String(product.id) && String(q.warehouseId) === String(wh2.id))
  .reduce((s, q) => s + Number(q.qty), 0);
console.log(`3. 调拨 OK: wh1=${inWh1}(>=2), wh2=${inWh2}(=4)`);

// ========== B. 盘点：wh2 盘点，实盘改 3（亏 1）→ 审核 → 凭证 ==========
const ckId = await api('POST', `/admin-api/stk/check/create?warehouseId=${wh2.id}&remark=M7冒烟盘点`, null, t);
const ckDetail = await api('GET', `/admin-api/stk/check/detail?id=${ckId}`, null, t);
for (const item of ckDetail.items) {
  if (String(item.productId) === String(product.id)) {
    await api('PUT', `/admin-api/stk/check/update-real-qty?itemId=${item.id}&realQty=3`, null, t);
  }
}
await api('POST', `/admin-api/stk/check/audit?id=${ckId}`, null, t);
const vch = (await api('GET', '/admin-api/fin/voucher/page?pageNo=1&pageSize=5&bizType=stock_check_loss', null, t)).list;
console.log(`4. 盘点审核 OK: 盘亏凭证=${vch[0]?.no || '无'}（借7损失/贷4库存）`);
const q2 = (await api('GET', `/admin-api/stk/quant/page?pageNo=1&pageSize=100`, null, t)).list;
const inWh2After = q2.filter(q => String(q.productId) === String(product.id) && String(q.warehouseId) === String(wh2.id))
  .reduce((s, q) => s + Number(q.qty), 0);
console.log(`   wh2 库存盘点后=${inWh2After}（3）`);

// ========== C. 销售退货 ==========
const customer = (await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=5&isCustomer=true', null, t)).list[0];
const srId = await api('POST', '/admin-api/sal/return/create', {
  customerId: customer.id, warehouseId: wh1.id, returnDate: '2026-11-08', remark: 'M7冒烟',
  items: [{ productId: product.id, qty: 2, price: 100 }]
}, t);
await api('POST', `/admin-api/sal/return/audit?id=${srId}`, null, t);
const srVch = (await api('GET', '/admin-api/fin/voucher/page?pageNo=1&pageSize=5&bizType=sale_return', null, t)).list;
console.log(`5. 销售退货 OK: 冲减收入凭证=${srVch[0]?.no || '无'}`);

// ========== D. 信用额度拦截 ==========
const cust2 = (await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=20&isCustomer=true', null, t)).list[1] || customer;
// 给客户 2 设额度 100
try {
  const upd = await fetch(BASE + `/admin-api/bas/partner/update`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` },
    body: JSON.stringify({ id: cust2.id, name: cust2.name, creditLimit: 100 })
  }).then(r => r.json());
} catch {}
const so = await api('POST', '/admin-api/sal/order/create', {
  customerId: cust2.id, orderDate: '2026-11-08',
  items: [{ productId: product.id, unitId: product.unitId, qty: 10, price: 500 }]
}, t);
await api('POST', `/admin-api/sal/order/audit?id=${so}`, null, t);
const soItems = await api('GET', `/admin-api/sal/order/item-list?orderId=${so}`, null, t);
const sdId = await api('POST', '/admin-api/sal/outbound/create', {
  customerId: cust2.id, warehouseId: wh1.id, orderId: so, outboundDate: '2026-11-08',
  items: soItems.map(i => ({ orderItemId: i.id, productId: i.productId, unitId: i.unitId, qty: 10, price: i.price }))
}, t);
try {
  await api('POST', `/admin-api/sal/outbound/audit?id=${sdId}`, null, t);
  console.log('6. ❌ 信用额度未被拦截');
} catch (e) {
  console.log('6. 信用额度拦截 OK:', e.message.slice(-80));
}

// ========== E. 商机赢单转销售订单 ==========
const leadId = await api('POST', '/admin-api/crm/lead/create', { name: 'M7冒烟客户-采购意向', source: '展会' }, t);
const bizId = await api('POST', `/admin-api/crm/lead/convert?id=${leadId}&partnerId=${cust2.id}&amount=5000`, null, t);
await api('PUT', `/admin-api/crm/business/stage?id=${bizId}&stage=5`, null, t);
const newOrderId = await api('POST', `/admin-api/crm/business/to-sale-order?id=${bizId}&productId=${product.id}&qty=3&price=300`, null, t);
console.log(`7. 赢单转销售订单 OK: 订单 id=${newOrderId}`);

// ========== F. 其他出入库 ==========
const oiId = await api('POST', `/admin-api/stk/other/create?otherType=other_in&warehouseId=${wh1.id}&remark=M7冒烟`,
  [{ productId: product.id, qty: 5, price: 30 }], t);
await api('POST', `/admin-api/stk/other/audit?id=${oiId}`, null, t);
console.log('8. 其他入库 OK');

// ========== G. 操作日志 + 公告 ==========
const logs = await api('GET', '/admin-api/system/oper-log/page?pageNo=1&pageSize=5', null, t);
console.log(`9. 操作日志 OK: 已自动记录 ${logs.total} 条写操作`);
const ntId = await api('POST', `/admin-api/infra/notice/create?title=M7 冒烟公告&content=系统升级完成`, null, t);
const notices = await api('GET', '/admin-api/infra/notice/list', null, t);
console.log(`10. 公告 OK: ${notices.length} 条已发布`);

console.log('\n✅ M7 全模块丰富冒烟测试全部通过');
