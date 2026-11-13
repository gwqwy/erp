const BASE = 'http://localhost:48080';
(async () => {
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;
  const warehouses = await fetch(BASE + '/admin-api/bas/warehouse/list', {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  const wh = warehouses[0];
  const ckId = await fetch(BASE + `/admin-api/stk/check/create?warehouseId=${wh.id}&remark=update验证`, {
    method: 'POST', headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  console.log('盘点单:', ckId);
  const detail = await fetch(BASE + `/admin-api/stk/check/detail?id=${ckId}`, {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  const item = detail.items[0];
  if (!item) { console.log('无明细行'); return; }
  console.log('行: book=', item.bookQty, 'real=', item.realQty);
  const upd = await fetch(BASE + `/admin-api/stk/check/update-real-qty?itemId=${item.id}&realQty=${Number(item.bookQty) + 2}`, {
    method: 'PUT', headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  console.log('update 返回:', JSON.stringify(upd));
  const after = await fetch(BASE + `/admin-api/stk/check/detail?id=${ckId}`, {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  console.log('更新后 real=', after.items[0].realQty);
})();
