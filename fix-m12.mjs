import fs from 'node:fs';
let s = fs.readFileSync('m12-smoke.mjs', 'utf8');
s = s.replace(
  "  const items = [{ productId: product.id, unitId: product.unitId, qty: 2, price: 10,",
  "  const partnersAll = await api('GET', '/admin-api/bas/partner/page?pageNo=1&pageSize=100', t);\n" +
  "  const supplier = (partnersAll.list || []).find(x => x.isSupplier);\n" +
  "  const customer = (partnersAll.list || []).find(x => x.isCustomer);\n" +
  "  if (!supplier || !customer) throw new Error('缺少供应商/客户数据');\n" +
  "  const items = [{ productId: product.id, unitId: product.unitId, qty: 2, price: 10,"
);
s = s.replace(
  "{ warehouseId: wh.id, supplierId: null, inboundDate: new Date().toISOString().slice(0, 10), items }",
  "{ warehouseId: wh.id, supplierId: supplier.id, inboundDate: new Date().toISOString().slice(0, 10), items }"
);
s = s.replace(
  "{ warehouseId: wh.id, outboundDate: new Date().toISOString().slice(0, 10), items }",
  "{ warehouseId: wh.id, customerId: customer.id, outboundDate: new Date().toISOString().slice(0, 10), items }"
);
fs.writeFileSync('m12-smoke.mjs', s, 'utf8');
console.log('smoke fixed:', s.includes('supplier.id') && s.includes('customerId: customer.id'));
