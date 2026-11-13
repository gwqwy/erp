const BASE = 'http://localhost:48080';
(async () => {
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;

  const url = BASE + '/admin-api/stk/check/create?warehouseId=2118783280850493442&remark=v10test';
  console.log('URL:', url);
  const res = await fetch(url, { method: 'POST', headers: { Authorization: 'Bearer ' + t } });
  console.log('status:', res.status);
  const body = await res.text();
  console.log('body:', body.slice(0, 400));
})();
