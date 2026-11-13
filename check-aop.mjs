const BASE = 'http://localhost:48080';
(async () => {
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;
  await fetch(BASE + '/admin-api/infra/notice/create?title=AOP验证公告', {
    method: 'POST', headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  const logs = await fetch(BASE + '/admin-api/system/oper-log/page?pageNo=1&pageSize=5', {
    headers: { Authorization: 'Bearer ' + t }
  }).then(r => r.json());
  console.log('操作日志 total:', logs.data.total);
  const row = logs.data.list[0];
  if (row) console.log('最新:', row.username, row.httpMethod, row.url, '成功=' + row.success);
})();
