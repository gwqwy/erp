import fs from 'node:fs';

// 扫描 src：所有页面 import 的 api 导出 vs api 文件实际 export，找缺失
const ui = 'E:/文件/编程文件/erp/tiangong-ui/src';
const apiDir = ui + '/api';
const apiExports = {};
for (const f of fs.readdirSync(apiDir)) {
  if (!f.endsWith('.ts')) continue;
  const s = fs.readFileSync(apiDir + '/' + f, 'utf8');
  const names = [...s.matchAll(/export (?:const|function) (\w+)/g)].map(m => m[1]);
  apiExports[f.replace('.ts', '')] = names;
}

const errors = [];
function walk(dir) {
  for (const f of fs.readdirSync(dir)) {
    const fp = dir + '/' + f;
    if (fs.statSync(fp).isDirectory()) { walk(fp); continue; }
    if (!f.endsWith('.vue') && !f.endsWith('.ts')) continue;
    const s = fs.readFileSync(fp, 'utf8');
    for (const m of s.matchAll(/import \{([^}]+)\} from '@\/api\/(\w+)'/g)) {
      const names = m[1].split(',').map(x => x.trim()).filter(Boolean);
      const file = m[2];
      for (const n of names) {
        if (!(apiExports[file] || []).includes(n)) {
          errors.push(`${fp.replace(ui + '/', '')}: ${n} 不在 api/${file}.ts`);
        }
      }
    }
  }
}
walk(ui + '/views');
walk(ui + '/components');
walk(ui + '/layout');

if (errors.length) {
  console.log('缺失导出 ' + errors.length + ' 处:');
  for (const e of errors) console.log('  ' + e);
} else {
  console.log('✅ 所有 api import 导出齐全');
}
