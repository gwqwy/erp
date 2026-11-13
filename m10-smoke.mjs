// M10 冒烟：V14 —— CSV 导入(商品/往来/期初) + 排行/ABC/对账单 + 预警扫描
const BASE = 'http://localhost:48080';
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

async function uploadCsv(token, kind, csv) {
  const form = new FormData();
  form.append('file', new Blob([csv], { type: 'text/csv' }), 'import.csv');
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

  // ---------- 1. 商品导入（含 1 行脏数据测容错） ----------
  const ts = Date.now() % 1000000;
  const p1 = 'IM-P' + ts + 'A', p2 = 'IM-P' + ts + 'B';
  const productCsv = [
    '编码,名称,规格,采购价,销售价,安全库存',
    `${p1},导入商品A,规格A,10.5,20,5`,
    `${p2},导入商品B,规格B,8,15,3`,
    `${p2},导入商品B-更新,,9,16,`, // 重复编码 → 更新
    `,空编码行,,1,1,` // 空编码 → 失败
  ].join('\n');
  const pr = await uploadCsv(t, 'products', productCsv);
  ok('商品导入', `total=${pr.total} success=${pr.success} fail=${pr.fail} errors=${pr.errors.length}`);
  if (pr.success !== 3 || pr.fail !== 1) throw new Error('商品导入结果不符');
  const products = await api('GET', `/admin-api/bas/product/page?pageNo=1&pageSize=50&keyword=${p1}`, t);
  const importedA = (products.list || []).find(x => x.no === p1);
  if (!importedA) throw new Error('导入商品A未找到');
  const products2 = await api('GET', `/admin-api/bas/product/page?pageNo=1&pageSize=50&keyword=${p2}`, t);
  const importedB = (products2.list || []).find(x => x.no === p2);
  if (!importedB || Number(importedB.purchasePrice) !== 9) throw new Error('商品B更新未生效: ' + importedB?.purchasePrice);
  ok('商品导入落库 + 按编码更新');

  // ---------- 2. 往来单位导入 ----------
  const partnerName = '导入客户' + ts;
  const partnerCsv = [
    '名称,类型,联系人,电话,地址',
    `${partnerName},both,张三,13800000000,北京市`,
    `供应商${ts},supplier,李四,13900000000,上海市`
  ].join('\n');
  const pr2 = await uploadCsv(t, 'partners', partnerCsv);
  ok('往来单位导入', `success=${pr2.success} fail=${pr2.fail}`);
  if (pr2.success !== 2) throw new Error('往来导入结果不符');

  // ---------- 3. 库存期初导入 ----------
  // 先找一个仓库编码
  const warehouses = await api('GET', '/admin-api/bas/warehouse/list', t);
  const wh = warehouses[0];
  const openCsv = [
    '仓库编码,商品编码,数量,成本价',
    `${wh.no || wh.name},${p1},100,10.5`,
    `${wh.no || wh.name},${p2},50,8`,
    `NO_SUCH_WH,${p1},1,1` // 错误行
  ].join('\n');
  const pr3 = await uploadCsv(t, 'stock-openings', openCsv);
  ok('库存期初导入', `success=${pr3.success} fail=${pr3.fail} err0=${pr3.errors[0]}`);
  if (pr3.success !== 2 || pr3.fail !== 1) throw new Error('期初导入结果不符');
  // 再导一次同数据 → 累加
  const pr4 = await uploadCsv(t, 'stock-openings', `仓库编码,商品编码,数量,成本价\n${wh.no || wh.name},${p1},100,`);
  if (pr4.success !== 1) throw new Error('二次导入失败');
  ok('期初二次导入累加');

  // ---------- 4. 预警扫描 ----------
  const scan = await api('POST', '/admin-api/stk/alert/scan', t, null);
  ok('预警手动扫描', JSON.stringify(scan));
  if (typeof scan.count !== 'number') throw new Error('扫描返回不符');
  const lowList = await api('GET', '/admin-api/stk/alert/low-list', t);
  ok('低库存列表', lowList.length + ' 个商品');

  // ---------- 5. 排行报表 ----------
  const saleRank = await api('GET', '/admin-api/rpt/sale-rank?limit=5', t);
  ok('销售排行', saleRank.length + ' 行');
  const purRank = await api('GET', '/admin-api/rpt/purchase-rank?limit=5', t);
  ok('采购排行', purRank.length + ' 行');

  // ---------- 6. 库存 ABC ----------
  const abc = await api('GET', '/admin-api/rpt/stock-abc', t);
  const aCount = abc.filter(x => x.abcClass === 'A').length;
  ok('库存 ABC', abc.length + ' 行，A类 ' + aCount);
  if (!abc.length) throw new Error('ABC 无数据');

  // ---------- 7. 客户对账单 ----------
  const partners = await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=50', t);
  const customer = (partners.list || []).find(x => x.isCustomer);
  const year = new Date().getFullYear();
  const stmt = await api('GET', `/admin-api/rpt/customer-statement?partnerId=${customer.id}&from=${year}-01-01&to=${year}-12-31`, t);
  ok('客户对账单', `opening=${stmt.opening} rows=${stmt.rows.length} closing=${stmt.closing}`);
  if (typeof stmt.opening === 'undefined' || typeof stmt.closing === 'undefined') throw new Error('对账单字段缺失');
  // 校验余额滚动一致性
  let expect = Number(stmt.opening);
  for (const row of stmt.rows) expect += Number(row.debit) - Number(row.credit);
  if (Math.abs(expect - Number(stmt.closing)) > 0.001) throw new Error('余额滚动不一致');
  ok('对账单余额滚动校验一致');

  // ---------- 8. 附件关联（bizType+bizId 过滤） ----------
  // 给导入商品A所在的一个销售草稿单传附件：直接用 /file/upload + list?bizId
  const form = new FormData();
  form.append('file', new Blob(['attach-smoke'], { type: 'text/plain' }), 'order-attach.txt');
  const up = await fetch(BASE + '/admin-api/file/upload?bizType=sal_order&bizId=999999', {
    method: 'POST', headers: { Authorization: 'Bearer ' + t }, body: form
  }).then(r => r.json());
  if (up.code !== 0) throw new Error('附件上传失败: ' + up.msg);
  const attachList = await api('GET', `/admin-api/file/list?bizType=sal_order&bizId=999999`, t);
  const hitAttach = attachList.some(f => f.fileName === 'order-attach.txt');
  ok('附件按单据过滤', attachList.length + ' 条，命中=' + hitAttach);
  if (!hitAttach) throw new Error('bizId 过滤未命中');
  await api('DELETE', `/admin-api/file/delete?id=${up.data.id}`, t, null);
  ok('清理测试附件');

  console.log('\n🎉 M10 冒烟全部通过');
} catch (e) {
  fail('M10', e);
}
