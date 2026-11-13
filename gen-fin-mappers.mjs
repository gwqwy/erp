import fs from 'node:fs';
import path from 'node:path';

const base = 'E:/文件/编程文件/erp/tiangong-erp/tiangong-module-fin/src/main/java/cn/tiangong/module/fin/dal/mysql';

const mappers = [
  ['ErpFinanceAccountMapper', 'ErpFinanceAccountDO'],
  ['ErpSubjectMapper', 'ErpSubjectDO'],
  ['ErpFinanceReceivableMapper', 'ErpFinanceReceivableDO'],
  ['ErpFinancePayableMapper', 'ErpFinancePayableDO'],
  ['ErpFinanceReceiptMapper', 'ErpFinanceReceiptDO'],
  ['ErpFinanceReceiptItemMapper', 'ErpFinanceReceiptItemDO'],
  ['ErpFinancePaymentMapper', 'ErpFinancePaymentDO'],
  ['ErpFinancePaymentItemMapper', 'ErpFinancePaymentItemDO'],
  ['ErpFinanceVoucherMapper', 'ErpFinanceVoucherDO'],
  ['ErpFinanceVoucherItemMapper', 'ErpFinanceVoucherItemDO'],
  ['ErpVoucherTemplateMapper', 'ErpVoucherTemplateDO']
];

for (const [mapper, doClass] of mappers) {
  const content = `package cn.tiangong.module.fin.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.fin.dal.dataobject.${doClass};

public interface ${mapper} extends BaseMapperX<${doClass}> {
}
`;
  fs.writeFileSync(path.join(base, mapper + '.java'), content, 'utf8');
  console.log('generated:', mapper);
}
console.log('done');
