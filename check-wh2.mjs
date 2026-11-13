const BASE = 'http://localhost:48080';
(async () => {
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;
  const res = await fetch(BASE + '/admin-api/bas/warehouse/list', {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  console.log('code:', res.code, 'msg:', res.msg);
  console.log('list:', JSON.stringify(res.data?.map(w => ({ id: w.id, name: w.name, status: w.status }))));
})();
