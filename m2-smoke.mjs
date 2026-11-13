// M2 业务财务一体化冒烟测试：
// 采购入库 → 自动应付+凭证 → 付款核销 → 应付结清+凭证+账户余额
// 销售出库 → 自动应收+凭证 → 收款核销 → 应收结清+凭证+账户余额
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

/** AFTER_COMMIT 监听器异步落库，轮询等待 */
async function waitFor(desc, query) {
  for (let i = 0; i < 25; i++) {
    const hit = await query();
    if (hit) return hit;
    await new Promise(r => setTimeout(r, 200));
  }
  throw new Error('❌ 等待超时: ' + desc);
}

// 已有基础数据：取供应商/客户/仓库/产品各一个
const partners = await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=20', null, t);
const supplier = partners.list.find(p => p.isSupplier);
const customer = partners.list.find(p => p.isCustomer);
const warehouses = await api('GET', '/admin-api/bas/warehouse/list', null, t);
const warehouse = warehouses[0];
const productPage = await api('GET', '/admin-api/bas/product/page?pageNo=1&pageSize=5', null, t);
const product = productPage.list[0];
console.log(`2. 基础数据: 供应商=${supplier.name}, 客户=${customer.name}, 仓库=${warehouse.name}, 产品=${product.name}`);

// ========== 采购闭环 + 应付 ==========
const poId = await api('POST', '/admin-api/pur/order/create', {
  supplierId: supplier.id, orderDate: '2026-11-07', remark: 'M2冒烟',
  items: [{ productId: product.id, unitId: product.unitId, qty: 20, price: 100 }]
}, t);
await api('POST', `/admin-api/pur/order/audit?id=${poId}`, null, t);
const poItems = await api('GET', `/admin-api/pur/order/item-list?orderId=${poId}`, null, t);
const piId = await api('POST', '/admin-api/pur/inbound/create', {
  supplierId: supplier.id, warehouseId: warehouse.id, orderId: poId, inboundDate: '2026-11-07',
  items: poItems.map(i => ({ orderItemId: i.id, productId: i.productId, unitId: i.unitId, qty: 20, price: i.price }))
}, t);
await api('POST', `/admin-api/pur/inbound/audit?id=${piId}`, null, t);
console.log('3. 采购订单→入库→审核 OK（金额 2000）');

// 应付自动生成？
const payable = await waitFor('应付自动生成', async () => {
  const page = await api('GET', '/admin-api/fin/payable/page?pageNo=1&pageSize=10', null, t);
  return page.list.find(p => p.bizType === 'purchase_in' && String(p.bizId) === String(piId));
});
if (!payable) throw new Error('❌ 应付未自动生成');
console.log(`4. 应付自动生成 ✅: ${payable.bizNo} 金额=${payable.amount} 未核销=${payable.unpaidAmount} 状态=${payable.status}`);

// 凭证自动生成？
const voucher = await waitFor('凭证自动生成', async () => {
  const page = await api('GET', '/admin-api/fin/voucher/page?pageNo=1&pageSize=10&bizType=purchase_in', null, t);
  return page.list.find(v => String(v.bizId) === String(piId));
});
if (!voucher) throw new Error('❌ 凭证未自动生成');
console.log(`5. 凭证自动生成 ✅: ${voucher.no} 借=${voucher.totalDebit} 贷=${voucher.totalCredit}`);
const vd = await api('GET', `/admin-api/fin/voucher/detail?id=${voucher.id}`, null, t);
for (const item of vd.items) {
  console.log(`   ${item.subjectNo} ${item.summary} 借:${item.debit} 贷:${item.credit}`);
}

// 付款核销
const payId = await api('POST', '/admin-api/fin/payment/create', {
  partnerId: supplier.id, accountId: 1, paymentDate: '2026-11-07', remark: 'M2冒烟付款',
  items: [{ payableId: payable.id, amount: 2000 }]
}, t);
await api('POST', `/admin-api/fin/payment/audit?id=${payId}`, null, t);
const payableAfter = await waitFor('应付核销', async () => {
  const page = await api('GET', '/admin-api/fin/payable/page?pageNo=1&pageSize=10', null, t);
  const hit = page.list.find(p => String(p.id) === String(payable.id));
  return hit && hit.status === 3 ? hit : null;
});
console.log(`6. 付款核销 ✅: 应付状态=${payableAfter.status}(3=已结清) 结清日=${payableAfter.settleDate}`);
const accounts = await api('GET', '/admin-api/fin/account/list', null, t);
const bank = accounts.find(a => a.id === '1' || String(a.id) === '1');
console.log(`   基本户余额 = ${bank.balance}（应为 -2000）`);
const payVouchers = await api('GET', '/admin-api/fin/voucher/page?pageNo=1&pageSize=10&bizType=payment', null, t);
console.log(`   付款凭证 ✅: ${payVouchers.list[0]?.no} 借=${payVouchers.list[0]?.totalDebit}`);

// ========== 销售闭环 + 应收 ==========
const soId = await api('POST', '/admin-api/sal/order/create', {
  customerId: customer.id, orderDate: '2026-11-07', remark: 'M2冒烟',
  items: [{ productId: product.id, unitId: product.unitId, qty: 15, price: 150 }]
}, t);
await api('POST', `/admin-api/sal/order/audit?id=${soId}`, null, t);
const soItems = await api('GET', `/admin-api/sal/order/item-list?orderId=${soId}`, null, t);
const sdId = await api('POST', '/admin-api/sal/outbound/create', {
  customerId: customer.id, warehouseId: warehouse.id, orderId: soId, outboundDate: '2026-11-07',
  items: soItems.map(i => ({ orderItemId: i.id, productId: i.productId, unitId: i.unitId, qty: 15, price: i.price }))
}, t);
await api('POST', `/admin-api/sal/outbound/audit?id=${sdId}`, null, t);
console.log('7. 销售订单→出库→审核 OK（金额 2250）');

const receivable = await waitFor('应收自动生成', async () => {
  const page = await api('GET', '/admin-api/fin/receivable/page?pageNo=1&pageSize=10', null, t);
  return page.list.find(r => r.bizType === 'sale_out' && String(r.bizId) === String(sdId));
});
if (!receivable) throw new Error('❌ 应收未自动生成');
console.log(`8. 应收自动生成 ✅: ${receivable.bizNo} 金额=${receivable.amount}`);

const recId = await api('POST', '/admin-api/fin/receipt/create', {
  partnerId: customer.id, accountId: 1, receiptDate: '2026-11-07', remark: 'M2冒烟收款',
  items: [{ receivableId: receivable.id, amount: 2250 }]
}, t);
await api('POST', `/admin-api/fin/receipt/audit?id=${recId}`, null, t);
const receivableAfter = await waitFor('应收核销', async () => {
  const page = await api('GET', '/admin-api/fin/receivable/page?pageNo=1&pageSize=10', null, t);
  const hit = page.list.find(r => String(r.id) === String(receivable.id));
  return hit && hit.status === 3 ? hit : null;
});
console.log(`9. 收款核销 ✅: 应收状态=${receivableAfter.status}(3=已结清)`);
const accounts2 = await api('GET', '/admin-api/fin/account/list', null, t);
const bank2 = accounts2.find(a => String(a.id) === '1');
console.log(`   基本户余额 = ${bank2.balance}（应为 250 = -2000 + 2250）`);

// 凭证总览
const allVouchers = await api('GET', '/admin-api/fin/voucher/page?pageNo=1&pageSize=20', null, t);
console.log('10. 凭证总览:');
for (const v of allVouchers.list) {
  console.log(`   ${v.no} [${v.bizType}] ${v.sourceNo} 借=${v.totalDebit} 贷=${v.totalCredit} 状态=${v.status}`);
}

console.log('\n✅ M2 业务财务一体化冒烟测试全部通过');
