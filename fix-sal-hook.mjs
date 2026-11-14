import fs from 'node:fs';
const p = 'tiangong-erp/tiangong-module-sal/src/main/java/cn/tiangong/module/sal/service/outbound/SaleOutboundServiceImpl.java';
let s = fs.readFileSync(p, 'utf8');
if (!s.includes('consumeSerialsIfAny(outbound.getWarehouseId(), outbound.getId(), outbound.getNo(), items);\n')) {
  const anchor = '        List<BigDecimal> costs = stockPostingApi.submit(commands);\n        BigDecimal costAmount = costs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);\n';
  if (!s.includes(anchor)) throw new Error('anchor missing');
  // 只替换第一处（auditOutbound 内）
  const insert = anchor + '\n        // 序列号核销：isSerial 商品明细 remark 中的序列号\n' +
    '        consumeSerialsIfAny(outbound.getWarehouseId(), outbound.getId(), outbound.getNo(), items);\n';
  s = s.replace(anchor, insert);
  fs.writeFileSync(p, s, 'utf8');
  console.log('sal hook inserted');
} else {
  console.log('sal hook already present');
}
