const BASE = 'http://localhost:48080';
const login = await fetch(BASE + '/admin-api/system/auth/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
}).then(r => r.json());
const t = login.data.accessToken;
const info = await fetch(BASE + '/admin-api/system/auth/get-permission-info', {
  headers: { Authorization: 'Bearer ' + t }
}).then(r => r.json());
for (const m of info.data.menus) {
  console.log('dir', m.id, m.name, 'path=' + m.path, 'children=' + (m.children || []).length);
}
// 检查 V2 item 表是否有 deleted 列
const fs = await import('node:fs');
const v2 = fs.readFileSync('tiangong-erp/tiangong-server/src/main/resources/db/migration/V2__m1_pur_sal_stk.sql', 'utf8');
const i = v2.indexOf('erp_sale_order_item');
console.log('---- erp_sale_order_item DDL ----');
console.log(v2.slice(i, i + 700));
