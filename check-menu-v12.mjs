const BASE = 'http://localhost:48080';
import fs from 'node:fs';
const viewSrc = fs.readFileSync('tiangong-ui/src/router/views.ts', 'utf8');
const login = await fetch(BASE + '/admin-api/system/auth/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
}).then(r => r.json());
const t = login.data.accessToken;
const info = await fetch(BASE + '/admin-api/system/auth/get-permission-info', {
  headers: { Authorization: 'Bearer ' + t }
}).then(r => r.json());
const missing = [];
const walk = menus => menus.forEach(m => {
  if (m.component && !viewSrc.includes(`'${m.component}'`)) {
    missing.push(`${m.id} ${m.name} -> ${m.component}`);
  }
  if (m.children) walk(m.children);
});
walk(info.data.menus);
console.log(missing.length ? missing.join('\n') : 'ALL-MENUS-MAPPED');
