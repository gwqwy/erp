// M4 冒烟测试：CRM（线索→转商机→阶段推进）+ MRP 计划落库→转采购 + 驾驶舱
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

// ========== CRM ==========
const leadId = await api('POST', '/admin-api/crm/lead/create', {
  name: '苏州精密制造厂-采购意向', source: '展会', contact: '王经理', phone: '13800001111'
}, t);
console.log('2. 线索创建 OK:', leadId);
const customers = await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=10&isCustomer=true', null, t);
const customer = customers.list[0];
const bizId = await api('POST', `/admin-api/crm/lead/convert?id=${leadId}&partnerId=${customer.id}&amount=80000&expectDate=2026-12-31`, null, t);
console.log('3. 线索转商机 OK:', bizId);
for (const stage of [2, 3, 4, 5]) {
  await api('PUT', `/admin-api/crm/business/stage?id=${bizId}&stage=${stage}`, null, t);
}
console.log('4. 商机漏斗推进 OK: 初步接触→需求确认→方案报价→商务谈判→赢单');
const stats = await api('GET', '/admin-api/crm/business/stage-stats', null, t);
console.log('   漏斗统计:', JSON.stringify(stats));

// 赢单终态不可再推进
try {
  await api('PUT', `/admin-api/crm/business/stage?id=${bizId}&stage=2`, null, t);
  console.log('   ❌ 赢单终态未被锁定');
} catch (e) {
  console.log('   赢单终态锁定 ✅:', e.message.slice(-30));
}

// ========== MRP 计划落库 + 转采购 ==========
const warehouses = await api('GET', '/admin-api/bas/warehouse/list', null, t);
const products = await api('GET', '/admin-api/bas/product/page?pageNo=1&pageSize=20', null, t);
const finished = products.list.find(p => p.name.includes('减速机'));
const plan = await api('POST', '/admin-api/mrp/plan/run-and-save', {
  warehouseId: warehouses[0].id,
  demands: [{ productId: finished.id, qty: 30 }],
  remark: 'M4冒烟'
}, t);
console.log(`5. MRP 试算落库 OK: ${plan.no} 状态=${plan.status}(1=已保存)`);
console.log('   净需求结果:', plan.resultJson.slice(0, 160), '...');

const supplier = (await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=10&isSupplier=true', null, t)).list[0];
const orderId = await api('POST', `/admin-api/mrp/plan/to-purchase-order?id=${plan.id}&supplierId=${supplier.id}`, null, t);
const poDetail = await api('GET', `/admin-api/pur/order/detail?id=${orderId}`, null, t);
console.log(`6. 计划转采购 OK: 采购订单 ${poDetail.order.no}，明细 ${poDetail.items.length} 行（草稿待审核）`);
const planPage = await api('GET', '/admin-api/mrp/plan/page?pageNo=1&pageSize=5', null, t);
const planAfter = planPage.list.find(p => String(p.id) === String(plan.id));
console.log(`   计划状态=${planAfter.status}(2=已转采购)`);

// ========== 驾驶舱 ==========
const dash = await api('GET', '/admin-api/rpt/dashboard/summary?months=6', null, t);
console.log('7. 驾驶舱聚合 OK:');
console.log(`   库存金额=${dash.stockValue} 未核销应收=${dash.unsettledReceivable} 未核销应付=${dash.unsettledPayable} 在产工单=${dash.activeWorkOrders}`);
console.log(`   销售趋势 ${dash.salesTrend.length} 月, 采购趋势 ${dash.purchaseTrend.length} 月`);

// ========== HR ==========
const empId = await api('POST', '/admin-api/hr/employee/create', {
  name: '张工', deptId: 3, post: '销售工程师', phone: '13911112222'
}, t);
console.log('8. 员工档案创建 OK:', empId);
const emps = await api('GET', '/admin-api/hr/employee/page?pageNo=1&pageSize=5', null, t);
console.log(`   员工列表 ${emps.total} 条`);

console.log('\n✅ M4 冒烟测试全部通过');
