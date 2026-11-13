const BASE = 'http://localhost:48080';
const login = await fetch(BASE + '/admin-api/system/auth/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
}).then(r => r.json());
const t = login.data.accessToken;
const d = await fetch(BASE + '/admin-api/sal/order/detail?id=2119438377166266369', {
  headers: { Authorization: 'Bearer ' + t }
}).then(r => r.json());
console.log('detail code:', d.code, 'keys:', d.data ? Object.keys(d.data) : null);
const pp = await fetch(BASE + '/admin-api/bas/product/page?pageNo=1&pageSize=500', {
  headers: { Authorization: 'Bearer ' + t }
}).then(r => r.json());
console.log('product page code:', pp.code, 'total:', pp.data ? pp.data.total : null, 'msg:', pp.msg);
