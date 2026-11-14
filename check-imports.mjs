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
    if (fp.includes('/api/')) continue;
    const s = fs.readFileSync(fp, 'utf8');
    for (const m of s.matchAll(/import\s+\{([^}]+)\}\s+from\s+'@\/api\/(\w+)'/g)) {
      const mod = m[2];
      if (!apiExports[mod]) { errors.push(fp + ': api 模块不存在 @/api/' + mod); continue; }
      for (const name of m[1].split(',').map(x => x.trim().split(' as ')[0]).filter(Boolean)) {
        if (!apiExports[mod].includes(name)) {
          errors.push(`${fp}: @/api/${mod} 缺少导出 ${name}`);
        }
      }
    }
  }
}
walk(ui);
console.log(errors.length ? errors.join('\n') : '✅ 所有 api import 导出齐全');
