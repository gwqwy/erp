// diag-final 输出 ckId 与 detail 原始 JSON
const BASE = 'http://localhost:48080';
(async () => {
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;
  const ck = await fetch(BASE + '/admin-api/stk/check/create?warehouseId=2118783280850493442&remark=d2', {
    method: 'POST', headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  console.log('create resp:', JSON.stringify(ck).slice(0, 120));
  const detail = await fetch(BASE + `/admin-api/stk/check/detail?id=${ck.data}`, {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  console.log('detail resp:', JSON.stringify(detail).slice(0, 300));
})();
