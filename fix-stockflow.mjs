import fs from 'node:fs';
const p = 'tiangong-ui/src/views/report/stock-flow/index.vue';
let s = fs.readFileSync(p, 'utf8');
const broken = "join(',')).join('" + String.fromCharCode(10) + "')";
const fixed = "join(',')).join('" + String.fromCharCode(92) + "n')";
if (s.includes(broken)) {
  s = s.replace(broken, fixed);
  fs.writeFileSync(p, s, 'utf8');
  console.log('fixed join');
} else {
  console.log('pattern not found');
  const i = s.indexOf(".join('");
  console.log(JSON.stringify(s.slice(i, i + 60)));
}
// 同样检查 Blob 里是否有断行
const blobBroken = "'\\uFEFF' + header + '" + String.fromCharCode(10) + "'";
if (s.includes(blobBroken)) {
  s = s.replace(blobBroken, "'\\uFEFF' + header + '\\n'");
  fs.writeFileSync(p, s, 'utf8');
  console.log('fixed blob');
}
