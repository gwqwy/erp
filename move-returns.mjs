import fs from 'node:fs';
import path from 'node:path';

const base = 'E:/文件/编程文件/erp/tiangong-erp';
const moves = [
  ['stk', 'sal', 'SaleReturnDO'],
  ['stk', 'sal', 'SaleReturnItemDO'],
  ['stk', 'pur', 'PurchaseReturnDO'],
  ['stk', 'pur', 'PurchaseReturnItemDO']
];

for (const [from, to, cls] of moves) {
  const fromPkg = `cn.tiangong.module.${from}.dal.dataobject`;
  const toPkg = `cn.tiangong.module.${to}.dal.dataobject`;
  const toMapperPkg = `cn.tiangong.module.${to}.dal.mysql`;

  const src = path.join(base, `tiangong-module-${from}`, 'src', 'main', 'java', 'cn', 'tiangong', 'module', from, 'dal', 'dataobject', cls + '.java');
  let s = fs.readFileSync(src, 'utf8');
  s = s.replace(`package ${fromPkg};`, `package ${toPkg};`);

  const toDir = path.join(base, `tiangong-module-${to}`, 'src', 'main', 'java', 'cn', 'tiangong', 'module', to, 'dal', 'dataobject');
  fs.mkdirSync(toDir, { recursive: true });
  fs.writeFileSync(path.join(toDir, cls + '.java'), s, 'utf8');
  fs.unlinkSync(src);

  const mapperName = cls.replace('DO', 'Mapper');
  const mc = `package ${toMapperPkg};

import ${toPkg}.${cls};
import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;

public interface ${mapperName} extends BaseMapperX<${cls}> {
}
`;
  const toMapperDir = path.join(base, `tiangong-module-${to}`, 'src', 'main', 'java', 'cn', 'tiangong', 'module', to, 'dal', 'mysql');
  fs.mkdirSync(toMapperDir, { recursive: true });
  fs.writeFileSync(path.join(toMapperDir, mapperName + '.java'), mc, 'utf8');
  const oldMapper = path.join(base, `tiangong-module-${from}`, 'src', 'main', 'java', 'cn', 'tiangong', 'module', from, 'dal', 'mysql', mapperName + '.java');
  if (fs.existsSync(oldMapper)) fs.unlinkSync(oldMapper);
  console.log('moved', cls, '->', to);
}
console.log('done');
