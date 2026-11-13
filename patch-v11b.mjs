import fs from 'node:fs';
const p = 'E:/文件/编程文件/erp/tiangong-erp/tiangong-server/src/main/resources/db/migration/V11__full_completion.sql';
let s = fs.readFileSync(p, 'utf8');
if (!s.includes('infra:export:csv')) {
  s += `\n-- CSV 导出权限\nINSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES (15180, 'CSV导出', 'infra:export:csv', 3, 1, 15174, '', '');\n`;
  fs.writeFileSync(p, s, 'utf8');
}
console.log('patched:', fs.readFileSync(p, 'utf8').includes('infra:export:csv'));
