import fs from 'node:fs';
const p = 'E:/文件/编程文件/erp/tiangong-ui/src/router/views.ts';
let s = fs.readFileSync(p, 'utf8');

const imports = `
import stkTransfer from '@/views/stk/transfer/index.vue'
import stkCheck from '@/views/stk/check/index.vue'
import stkAlert from '@/views/stk/alert/index.vue'
import stkOtherStock from '@/views/stk/other-stock/index.vue'
import salReturn from '@/views/sal/return/index.vue'
import purReturn from '@/views/pur/return/index.vue'
import finExpense from '@/views/fin/expense/index.vue'
import finAging from '@/views/fin/aging/index.vue'
import moreNotice from '@/views/more/notice/index.vue'
import systemOperLog from '@/views/system/oper-log/index.vue'
`;
const entries = `
  'stk/transfer/index': stkTransfer,
  'stk/check/index': stkCheck,
  'stk/alert/index': stkAlert,
  'stk/other-stock/index': stkOtherStock,
  'sal/return/index': salReturn,
  'pur/return/index': purReturn,
  'fin/expense/index': finExpense,
  'fin/aging/index': finAging,
  'more/notice/index': moreNotice,
  'system/oper-log/index': systemOperLog,
`;

s = s.replace('export const viewModules', imports + '\nexport const viewModules');
s = s.replace(/(\n}\n?$)/, entries + '$1');
fs.writeFileSync(p, s, 'utf8');
console.log('views.ts updated:', s.includes('stkTransfer') && s.includes("'system/oper-log/index'"));
