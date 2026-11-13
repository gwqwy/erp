import fs from 'node:fs';
import path from 'node:path';

const base = 'E:/文件/编程文件/erp/tiangong-erp/tiangong-module-mrp/src/main/java/cn/tiangong/module/mrp/dal/mysql';

const mappers = [
  ['ErpMrpBomMapper', 'ErpMrpBomDO'],
  ['ErpMrpBomItemMapper', 'ErpMrpBomItemDO'],
  ['ErpMrpWorkOrderMapper', 'ErpMrpWorkOrderDO'],
  ['ErpMrpWorkOrderItemMapper', 'ErpMrpWorkOrderItemDO'],
  ['ErpMrpMaterialReqMapper', 'ErpMrpMaterialReqDO'],
  ['ErpMrpMaterialReqItemMapper', 'ErpMrpMaterialReqItemDO']
];

for (const [mapper, doClass] of mappers) {
  const content = `package cn.tiangong.module.mrp.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.mrp.dal.dataobject.${doClass};

public interface ${mapper} extends BaseMapperX<${doClass}> {
}
`;
  fs.writeFileSync(path.join(base, mapper + '.java'), content, 'utf8');
  console.log('generated:', mapper);
}
console.log('done');
