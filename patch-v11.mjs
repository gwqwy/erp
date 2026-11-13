import fs from 'node:fs';
const p = 'E:/文件/编程文件/erp/tiangong-erp/tiangong-server/src/main/resources/db/migration/V11__full_completion.sql';
let s = fs.readFileSync(p, 'utf8');
if (!s.includes('infra:export:csv')) {
  const insert = `-- 导出权限
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15180, 'CSV导出', 'infra:export:csv', 3, 1, 15174, '', '');

`;
  s = s.replace('-- 单号规则', insert + '-- 单号规则');
  fs.writeFileSync(p, s, 'utf8');
}
console.log('patched:', s.includes('infra:export:csv'));
