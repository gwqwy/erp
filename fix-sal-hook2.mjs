import fs from 'node:fs';
const p = 'tiangong-erp/tiangong-module-sal/src/main/java/cn/tiangong/module/sal/service/outbound/SaleOutboundServiceImpl.java';
let s = fs.readFileSync(p, 'utf8');
// 1) 移除 reverseOutbound 里的误插调用
const misplaced = '\n        // 序列号核销：isSerial 商品明细 remark 中的序列号\n' +
  '        consumeSerialsIfAny(outbound.getWarehouseId(), outbound.getId(), outbound.getNo(), items);\n';
if (s.includes(misplaced)) {
  s = s.replace(misplaced, '\n');
  console.log('misplaced call removed');
}
// 2) 插入到 auditOutbound 的过账后（185 行上下文）
const anchor = '        List<BigDecimal> costs = stockPostingApi.submit(commands);\n        BigDecimal costAmount = costs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);\n';
if (!s.includes(anchor)) throw new Error('audit anchor missing');
const insert = anchor + '\n        // 序列号核销：isSerial 商品明细 remark 中的序列号\n' +
  '        consumeSerialsIfAny(outbound.getWarehouseId(), outbound.getId(), outbound.getNo(), items);\n';
s = s.replace(anchor, insert);
fs.writeFileSync(p, s, 'utf8');
// 3) 校验调用位置在 auditOutbound 之后、reverseOutbound 之前
const auditIdx = s.indexOf('public void auditOutbound');
const callIdx = s.indexOf('consumeSerialsIfAny(outbound.getWarehouseId()');
const reverseIdx = s.indexOf('public void reverseOutbound');
console.log('audit < call < reverse:', auditIdx < callIdx && callIdx < reverseIdx);
