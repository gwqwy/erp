const BASE = 'http://localhost:5173';
(async () => {
  for (const f of ['OrderPage.vue', 'CrudPage.vue', 'TreeCrudPage.vue']) {
    const res = await fetch(BASE + '/src/components/' + f);
    console.log((res.status === 200 ? '✅ 200' : '❌ ' + res.status), f);
  }
  const home = await fetch(BASE + '/').then(r => r.status);
  console.log('home:', home);
})();
