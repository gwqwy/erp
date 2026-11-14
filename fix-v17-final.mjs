import fs from 'node:fs';
let s = fs.readFileSync('gen-v17-backend.mjs', 'utf8');
// 折叠重复前缀
s = s.split('tiangong-erp/tiangong-erp/').join('tiangong-erp/');
// base 保持 '.'
s = s.replace("const base = 'tiangong-erp';", "const base = '.';");
// 锚点统一
s = s.split('voidSaleOrder').join('voidOrder');
fs.writeFileSync('gen-v17-backend.mjs', s, 'utf8');
// 校验
const problems = [];
if (s.includes('tiangong-erp/tiangong-erp/')) problems.push('still double-prefixed');
if (!s.includes("const base = '.';")) problems.push('base not dot');
if ((s.match(/voidOrder/g) || []).length < 6) problems.push('anchors missing');
console.log(problems.length ? 'PROBLEMS: ' + problems.join('; ') : 'script clean');
// 打出前几个 patch 路径供人工核对
for (const m of s.matchAll(/patch\('([^']+)'/g)) console.log('  patch:', m[1]);
for (const m of s.matchAll(/write\('([^']+)'/g)) console.log('  write:', m[1]);
