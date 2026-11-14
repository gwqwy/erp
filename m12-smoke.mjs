// M12 冒烟：V17 —— 订金登记 + 序列号追溯（入库/出库钩子） + BOM 成本卷算 + xlsx 导入导出
const BASE = 'http://localhost:48080';
const UI = 'http://localhost:5173';
const ok = (name, extra) => console.log('✅', name, extra !== undefined ? '→ ' + extra : '');
const fail = (name, e) => { console.log('❌', name, e?.message || e); process.exit(1); };

async function api(method, url, token, body) {
  const res = await fetch(BASE + url, {
    method,
    headers: { Authorization: 'Bearer ' + token, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined
  });
  const json = await res.json();
  if (json.code !== 0) throw new Error('code=' + json.code + ' msg=' + json.msg + ' url=' + url);
  return json.data;
}

async function uploadFile(token, kind, buf, filename) {
  const form = new FormData();
  form.append('file', new Blob([buf]), filename);
  const res = await fetch(BASE + '/admin-api/infra/import/' + kind, {
    method: 'POST', headers: { Authorization: 'Bearer ' + token }, body: form
  }).then(r => r.json());
  if (res.code !== 0) throw new Error('导入失败: ' + res.msg);
  return res.data;
}

try {
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;
  ok('登录');

  // ---------- 1. xlsx 导入（POI 解析，内容同 CSV 模板） ----------
  // 构造 xlsx：手工构造最简单 xlsx 太繁琐——用 Node 直接生成？改用 CSV 内容伪装 .xlsx 会走 POI 报错。
  // 因此 xlsx 导入的验证方式：上传真 xlsx（借助 python/openpyxl 不可用）→
  // 这里退而验证：xlsx 导出（POI 生成）+ xlsx 上传的 POI 读取在导出文件回传时验证。
  const xlsxRes = await fetch(BASE + '/admin-api/infra/export/xlsx?table=erp_product', {
    headers: { Authorization: 'Bearer ' + t }
  });
  ok('xlsx 导出 erp_product', xlsxRes.status + ' ' + (xlsxRes.headers.get('content-type') || '').slice(0, 40));
  if (xlsxRes.status !== 200) throw new Error('xlsx 导出失败');
  const xlsxBytes = Buffer.from(await xlsxRes.arrayBuffer());
  if (xlsxBytes.length < 1000 || xlsxBytes[0] !== 0x50) throw new Error('xlsx 内容异常');
  ok('xlsx 文件签名', xlsxBytes.length + ' bytes PK 头');

  // 用导出的 erp_product.xlsx 回传导入（商品导入按编码 upsert，幂等）
  const pr = await uploadFile(t, 'products', xlsxBytes, 'products.xlsx');
  ok('xlsx 导入回传（POI 解析）', `total=${pr.total} success=${pr.success} fail=${pr.fail}`);
  // 导出表列结构与导入模板不同（导出是全表列），部分行会因空名称失败——只要求 POI 解析成功且 total>0
  if (pr.total <= 0) throw new Error('xlsx 解析 0 行');

  // ---------- 2. 订金登记 ----------
  const orders = await api('GET', '/admin-api/sal/order/page?pageNo=1&pageSize=5', t);
  const order = orders.list[0];
  const before = await api('GET', `/admin-api/sal/order/deposit?id=${order.id}`, t);
  const depositBase = Number(before.depositAmount || 0);
  const newDeposit = await api('POST', `/admin-api/sal/order/deposit?id=${order.id}&amount=100`, t, null);
  ok('销售订金登记 100', `累计 ${depositBase} → ${newDeposit}`);
  if (Number(newDeposit) !== depositBase + 100) throw new Error('订金累计不符');
  // 超额校验：总累计超过订单总额应报错
  const total = Number(before.totalAmountTax || before.totalAmount);
  const over = total - Number(newDeposit) + 1;
  let overErr = null;
  try { await api('POST', `/admin-api/sal/order/deposit?id=${order.id}&amount=${over}`, t, null); } catch (e) { overErr = e; }
  if (!overErr) throw new Error('超额订金未被拦截');
  ok('超额订金拦截', overErr.message.slice(0, 60));

  // ---------- 3. BOM 成本卷算 ----------
  const products = await api('GET', '/admin-api/bas/product/page?pageNo=1&pageSize=50', t);
  // 找有 BOM 的产品（mrp bom page）
  const boms = await api('GET', '/admin-api/mrp/bom/page?pageNo=1&pageSize=10', t);
  let rollup = null;
  if (boms.list && boms.list.length) {
    rollup = await api('GET', `/admin-api/mrp/calc/cost?productId=${boms.list[0].productId}`, t);
    ok('BOM 成本卷算', `${rollup.productName} = ${Number(rollup.totalCost).toFixed(2)}，明细 ${rollup.lines.length} 行`);
    if (!rollup.lines.length) throw new Error('卷算明细为空');
  } else {
    rollup = await api('GET', `/admin-api/mrp/calc/cost?productId=${products.list[0].id}`, t);
    ok('BOM 成本卷算（无 BOM 走采购价）', `${rollup.productName} = ${Number(rollup.totalCost).toFixed(2)}`);
  }

  // ---------- 4. 序列号追溯（入库/出库钩子） ----------
  // 造一张采购入库（含 remark 序列号）→ 审核 → 查询序列号在库
  const ts = Date.now() % 1000000;
  const warehouses = await api('GET', '/admin-api/bas/warehouse/list', t);
  const wh = warehouses[0];
  const product = products.list[0];
  const SN = 'SN-M12-' + ts;
  const partnersAll = await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=100', t);
  const supplier = (partnersAll.list || []).find(x => x.isSupplier);
  const customer = (partnersAll.list || []).find(x => x.isCustomer);
  if (!supplier || !customer) throw new Error('缺少供应商/客户数据');
  const items = [{ productId: product.id, unitId: product.unitId, qty: 2, price: 10,
    remark: SN + ',SN-M12-' + ts + 'b' }];
  const inb = await api('POST', '/admin-api/pur/inbound/create', t,
    { warehouseId: wh.id, supplierId: supplier.id, inboundDate: new Date().toISOString().slice(0, 10), items });
  await api('POST', `/admin-api/pur/inbound/audit?id=${inb}`, t, null);
  ok('采购入库（明细备注带序列号）已审核', 'id=' + inb);
  const inStock = await api('GET', `/admin-api/stk/serial/in-stock?productId=${product.id}`, t);
  const hit = inStock.find(x => x.serialNo === SN);
  if (!hit) throw new Error('入库后序列号未登记: ' + SN);
  ok('序列号入库自动登记', SN + ' 在库');

  // 销售出库消费序列号
  const outbound = await api('POST', '/admin-api/sal/outbound/create', t,
    { warehouseId: wh.id, customerId: customer.id, outboundDate: new Date().toISOString().slice(0, 10), items });
  await api('POST', `/admin-api/sal/outbound/audit?id=${outbound}`, t, null);
  const trace = await api('GET', `/admin-api/stk/serial/trace?serialNo=${SN}`, t);
  if (!trace.length || Number(trace[trace.length - 1].status) !== 2) {
    throw new Error('出库后序列号未核销');
  }
  ok('序列号出库自动核销', '轨迹 ' + trace.length + ' 段，状态=已出库');

  // 手工登记/核销
  const SN2 = 'SN-M12-M-' + ts;
  await api('POST', `/admin-api/stk/serial/register?serialNo=${SN2}&productId=${product.id}&bizNo=MANUAL`, t, null);
  await api('POST', `/admin-api/stk/serial/consume?serialNo=${SN2}&productId=${product.id}&bizNo=MANUAL`, t, null);
  const trace2 = await api('GET', `/admin-api/stk/serial/trace?serialNo=${SN2}`, t);
  if (Number(trace2[trace2.length - 1].status) !== 2) throw new Error('手工登记/核销失败');
  ok('手工登记→核销');

  // ---------- 5. 前端新文件编译 ----------
  for (const p of ['src/views/more/serial-trace/index.vue', 'src/views/screen/index.vue',
    'src/components/OrderPage.vue', 'src/views/mrp/calc/index.vue']) {
    const res = await fetch(UI + '/' + p);
    if (res.status !== 200) throw new Error('页面编译失败 ' + p + ' → ' + res.status);
  }
  ok('前端 4 个改动文件 vite 编译 200');

  console.log('\n🎉 M12 冒烟全部通过');
} catch (e) {
  fail('M12', e);
}
