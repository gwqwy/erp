const BASE = 'http://localhost:48080';
async function api(method, url, body, token) {
  const res = await fetch(BASE + url, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: body ? JSON.stringify(body) : undefined
  });
  const json = await res.json();
  if (json.code !== 0) throw new Error(`${url} ${json.code} ${json.msg}`);
  return json.data;
}
(async () => {
  const login = await api('POST', '/admin-api/system/auth/login', { username: 'admin', password: 'admin123' });
  const t = login.accessToken;
  const warehouses = await api('GET', '/admin-api/bas/warehouse/list', null, t);
  const products = await api('GET', '/admin-api/bas/product/page?pageNo=1&pageSize=20', null, t);
  const finished = products.list.find(p => p.name.includes('减速机'));
  const calc = await api('POST', '/admin-api/mrp/calc/run', {
    warehouseId: warehouses[0].id,
    demands: [{ productId: finished.id, qty: 10 }]
  }, t);
  console.log('calc result:', JSON.stringify(calc, null, 2));
})().catch(e => console.error('ERR', e.message));
