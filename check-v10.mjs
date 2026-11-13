const BASE = 'http://localhost:48080';
(async () => {
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;

  // 1. 触发一次写操作（发公告）
  await fetch(BASE + '/admin-api/infra/notice/create?title=加固验证', {
    method: 'POST', headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());

  // 2. 操作日志应已记录
  const logs = await fetch(BASE + '/admin-api/system/oper-log/page?pageNo=1&pageSize=3', {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  console.log('操作日志 total:', logs.data.total);
  const row = logs.data.list[0];
  if (row) console.log('最新:', row.username, row.httpMethod, row.url, 'success=' + row.success, 'cost=' + row.costMs + 'ms');

  // 3. 盘点 update-real-qty 加固验证
  const warehouses = await fetch(BASE + '/admin-api/bas/warehouse/list', {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  const wh = warehouses.data[0];
  const ckId = await fetch(BASE + `/admin-api/stk/check/create?warehouseId=${wh.id}&remark=v10test`, {
    method: 'POST', headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  const detail = await fetch(BASE + `/admin-api/stk/check/detail?id=${ckId}`, {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  const item = detail.items[0];
  if (!item) { console.log('盘点单无明细行（空仓）'); return; }
  await fetch(BASE + `/admin-api/stk/check/update-real-qty?itemId=${item.id}&realQty=${Number(item.bookQty) + 2}`, {
    method: 'PUT', headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  const after = await fetch(BASE + `/admin-api/stk/check/detail?id=${ckId}`, {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  console.log(`盘点 update-real-qty: ${Number(item.bookQty)} -> ${after.items[0].realQty} ${Number(after.items[0].realQty) === Number(item.bookQty) + 2 ? '✅' : '❌'}`);
})();
