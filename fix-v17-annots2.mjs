import fs from 'node:fs';
// voidOrder 补回注解
const files = [
  'tiangong-erp/tiangong-module-pur/src/main/java/cn/tiangong/module/pur/service/order/PurchaseOrderServiceImpl.java',
  'tiangong-erp/tiangong-module-sal/src/main/java/cn/tiangong/module/sal/service/order/SaleOrderServiceImpl.java'
];
for (const p of files) {
  let s = fs.readFileSync(p, 'utf8');
  const i = s.indexOf('    public void voidOrder(');
  const before = s.slice(Math.max(0, i - 40), i);
  if (!before.includes('@Override')) {
    s = s.slice(0, i) + '    @Override\n    @Transactional(rollbackFor = Exception.class)\n' + s.slice(i);
    fs.writeFileSync(p, s, 'utf8');
    console.log('restored voidOrder annotations:', p.split('module-')[1]);
  } else {
    console.log('voidOrder has annotations:', p.split('module-')[1]);
  }
}
