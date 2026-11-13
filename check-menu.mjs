const BASE = 'http://localhost:48080';
(async () => {
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;
  const info = await fetch(BASE + '/admin-api/system/auth/get-permission-info', {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  const hr = info.data.menus.find(m => m.name === '人事管理');
  if (!hr) { console.log('人事管理菜单不存在！全部菜单:', info.data.menus.map(m => m.name).join(' | ')); return; }
  console.log('人事管理子菜单:', hr.children.map(c => c.name).join(' | '));
})();
