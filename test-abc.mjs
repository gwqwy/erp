const BASE = 'http://localhost:48080';
const login = await fetch(BASE + '/admin-api/system/auth/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
}).then(r => r.json());
const t = login.data.accessToken;
const abc = await fetch(BASE + '/admin-api/rpt/stock-abc', {
  headers: { Authorization: 'Bearer ' + t }
}).then(r => r.json());
console.log('rows:', abc.data.length);
console.log('first row:', JSON.stringify(abc.data[0]));
