import fs from 'node:fs';
const files = [
  'tiangong-erp/tiangong-module-pur/src/main/java/cn/tiangong/module/pur/service/order/PurchaseOrderServiceImpl.java',
  'tiangong-erp/tiangong-module-sal/src/main/java/cn/tiangong/module/sal/service/order/SaleOrderServiceImpl.java'
];
for (const p of files) {
  let s = fs.readFileSync(p, 'utf8');
  const bad = '    @Override\n    @Transactional(rollbackFor = Exception.class)\n    @Override\n    @Transactional(rollbackFor = Exception.class)\n    public void updateDeposit(';
  const good = '    @Override\n    @Transactional(rollbackFor = Exception.class)\n    public void updateDeposit(';
  if (s.includes(bad)) {
    s = s.replace(bad, good);
    fs.writeFileSync(p, s, 'utf8');
    console.log('fixed dup:', p.split('module-')[1]);
  } else {
    console.log('no dup:', p.split('module-')[1]);
  }
}
// voidOrder 方法是否丢了 @Override？检查插入点之后的 voidOrder 是否还有注解
for (const p of files) {
  const s = fs.readFileSync(p, 'utf8');
  const i = s.indexOf('public void voidOrder(');
  const before = s.slice(Math.max(0, i - 220), i);
  console.log(p.split('module-')[1], 'voidOrder 前置:', JSON.stringify(before.slice(-180)));
}
