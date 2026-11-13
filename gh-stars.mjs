const repos = [
  'dromara/free-fs',
  'bluexsx/box-im',
  'jingyulee/spring-file-storage',
  'caojiantao/springboot-websocket',
  'dromara/x-file-storage',
  'odoo/odoo'
];
(async () => {
  for (const r of repos) {
    try {
      const res = await fetch('https://api.github.com/repos/' + r);
      const j = await res.json();
      console.log(
        (j.full_name || r).padEnd(32),
        '⭐' + String(j.stargazers_count ?? '?').padEnd(6),
        '| 更新', (j.pushed_at || '').slice(0, 10),
        '|', (j.description || '-').slice(0, 60)
      );
    } catch (e) {
      console.log(r, 'ERR', e.message);
    }
  }
})();
