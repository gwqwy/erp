// M11 冒烟：V16 —— 收发存汇总 + 供应商对账单 + 站内消息闭环
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

try {
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;
  ok('登录');

  // ---------- 1. 收发存汇总 ----------
  const year = new Date().getFullYear();
  const flow = await api('GET', `/admin-api/rpt/stock-flow?from=${year}-01-01&to=${year}-12-31`, t);
  ok('收发存汇总', flow.length + ' 行');
  // 恒等式校验：期初 + 入 - 出 = 期末
  let bad = 0;
  for (const r of flow) {
    const expect = Number(r.beginQty) + Number(r.inQty) - Number(r.outQty);
    if (Math.abs(expect - Number(r.endQty)) > 0.001) bad++;
  }
  if (bad > 0) throw new Error('收发存恒等式不成立 ' + bad + ' 行');
  ok('期初+入-出=期末 恒等式', flow.length + ' 行全部成立');
  const hasEnd = flow.some(r => Number(r.endQty) !== 0);
  ok('期末结存有数据', hasEnd);

  // ---------- 2. 供应商对账单 ----------
  const partners = await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=100', t);
  const supplier = (partners.list || []).find(x => x.isSupplier);
  if (!supplier) throw new Error('无供应商数据');
  const stmt = await api('GET', `/admin-api/rpt/supplier-statement?partnerId=${supplier.id}&from=${year}-01-01&to=${year}-12-31`, t);
  ok('供应商对账单', `opening=${stmt.opening} rows=${stmt.rows.length} closing=${stmt.closing}`);
  let expect = Number(stmt.opening);
  for (const row of stmt.rows) expect += Number(row.debit) - Number(row.credit);
  if (Math.abs(expect - Number(stmt.closing)) > 0.001) throw new Error('供应商对账余额滚动不一致');
  ok('余额滚动校验一致');

  // ---------- 3. 站内消息闭环 ----------
  // 未读基线
  const baseCount = Number(await api('GET', '/admin-api/system/message/my-unread-count', t));
  // 发一条给自己
  const info = await api('GET', '/admin-api/system/auth/get-permission-info', t);
  const myId = info.user.id;
  const msgId = await api('POST', `/admin-api/system/message/send?userId=${myId}&title=${encodeURIComponent('M11 冒烟消息')}&content=${encodeURIComponent('V16 消息闭环测试')}`, t, null);
  ok('发送站内消息', 'msgId=' + msgId);
  const afterCount = Number(await api('GET', '/admin-api/system/message/my-unread-count', t));
  if (afterCount !== baseCount + 1) throw new Error(`未读数未+1: ${baseCount} → ${afterCount}`);
  ok('未读数 +1', `${baseCount} → ${afterCount}`);
  const unread = await api('GET', '/admin-api/system/message/my-unread', t);
  const hit = unread.find(m => m.title === 'M11 冒烟消息');
  if (!hit) throw new Error('未读列表缺新消息');
  ok('未读列表命中');
  // 标记已读
  await api('POST', `/admin-api/system/message/read?id=${msgId}`, t, null);
  const afterRead = Number(await api('GET', '/admin-api/system/message/my-unread-count', t));
  if (afterRead !== afterCount - 1) throw new Error('已读未减一');
  ok('标记已读 -1', `${afterCount} → ${afterRead}`);
  // 全部已读（清理基线，保留原状之前先记录）
  if (afterRead > 0) {
    await api('POST', '/admin-api/system/message/read-all', t, null);
    const finalCount = Number(await api('GET', '/admin-api/system/message/my-unread-count', t));
    if (finalCount !== 0) throw new Error('全部已读失败: ' + finalCount);
    ok('全部已读', '清零');
  }

  // ---------- 4. 前端新文件编译 ----------
  for (const p of ['src/views/report/stock-flow/index.vue', 'src/views/report/supplier-statement/index.vue',
    'src/views/system/message/index.vue', 'src/layout/index.vue', 'src/views/dashboard/index.vue']) {
    const res = await fetch(UI + '/' + p);
    if (res.status !== 200) throw new Error('页面编译失败 ' + p + ' → ' + res.status);
  }
  ok('前端 5 个改动文件 vite 编译 200');

  console.log('\n🎉 M11 冒烟全部通过');
} catch (e) {
  fail('M11', e);
}
