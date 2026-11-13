const BASE = 'http://localhost:48080';
(async () => {
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;

  // 盘点 update-real-qty 完整验证
  const ckId = await fetch(BASE + '/admin-api/stk/check/create?warehouseId=2118783280850493442&remark=final', {
    method: 'POST', headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json()).then(j => j.data);
  const detail = await fetch(BASE + `/admin-api/stk/check/detail?id=${ckId}`, {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  const item = detail.items[0];
  if (!item) { console.log('盘点单无明细'); return; }
  await fetch(BASE + `/admin-api/stk/check/update-real-qty?itemId=${item.id}&realQty=${Number(item.bookQty) + 2}`, {
    method: 'PUT', headers: { Authorization: 'Bearer ' + t }
  });
  const after = await fetch(BASE + `/admin-api/stk/check/detail?id=${ckId}`, {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  const ok = Number(after.items[0].realQty) === Number(item.bookQty) + 2;
  console.log(`1. 盘点 update-real-qty: ${item.bookQty} -> ${after.items[0].realQty} ${ok ? '✅' : '❌'}`);
})();
