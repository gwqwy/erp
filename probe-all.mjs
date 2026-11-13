// 全量探测：所有 views 页面模块的编译状态
const base = 'http://localhost:5173/src/';
const modules = [
  'main.ts', 'router/views.ts', 'router/index.ts', 'stores/menu.ts',
  'views/dashboard/index.vue',
  'views/login/index.vue',
  'views/system/user/index.vue', 'views/system/role/index.vue', 'views/system/menu/index.vue',
  'views/system/dept/index.vue', 'views/system/dict/index.vue', 'views/system/oper-log/index.vue',
  'views/bas/product/index.vue', 'views/bas/product-category/index.vue', 'views/bas/unit/index.vue',
  'views/bas/partner/index.vue', 'views/bas/warehouse/index.vue', 'views/bas/serial-rule/index.vue',
  'views/bas/location/index.vue', 'views/bas/partner-price/index.vue',
  'views/pur/order/index.vue', 'views/pur/inbound/index.vue', 'views/pur/return/index.vue',
  'views/sal/order/index.vue', 'views/sal/outbound/index.vue', 'views/sal/return/index.vue',
  'views/stk/quant/index.vue', 'views/stk/move/index.vue', 'views/stk/transfer/index.vue',
  'views/stk/check/index.vue', 'views/stk/alert/index.vue', 'views/stk/other-stock/index.vue',
  'views/fin/receivable/index.vue', 'views/fin/payable/index.vue', 'views/fin/receipt/index.vue',
  'views/fin/payment/index.vue', 'views/fin/voucher/index.vue', 'views/fin/voucher-template/index.vue',
  'views/fin/expense/index.vue', 'views/fin/aging/index.vue', 'views/fin/invoice/index.vue',
  'views/fin/period-close/index.vue', 'views/fin/receivable-plan/index.vue',
  'views/mrp/bom/index.vue', 'views/mrp/work-order/index.vue', 'views/mrp/material-req/index.vue',
  'views/mrp/calc/index.vue', 'views/mrp/plan/index.vue', 'views/mrp/quality-check/index.vue',
  'views/crm/lead/index.vue', 'views/crm/business/index.vue',
  'views/hr/employee/index.vue', 'views/hr/leave/index.vue', 'views/hr/salary/index.vue',
  'views/hr/attendance/index.vue', 'views/hr/attendance-summary/index.vue',
  'views/hr/overtime/index.vue', 'views/hr/trip/index.vue', 'views/hr/resignation/index.vue',
  'views/hr/reward-penalty/index.vue', 'views/hr/contract/index.vue',
  'views/hr/payroll/index.vue', 'views/hr/recruit/index.vue', 'views/hr/training/index.vue',
  'views/hr/performance/index.vue',
  'views/bpm/my-tasks/index.vue', 'views/bpm/config/index.vue',
  'views/report/balance/index.vue', 'views/report/profit/index.vue',
  'views/more/batch-trace/index.vue', 'views/more/quality-check/index.vue',
  'views/more/dynamic-form/index.vue', 'views/more/notice/index.vue',
  'views/more/recycle/index.vue', 'views/more/message/index.vue'
];

let bad = 0;
for (const m of modules) {
  try {
    const res = await fetch(base + m);
    if (res.status !== 200) {
      const body = await res.text();
      console.log(`❌ ${res.status} ${m}`);
      const errLine = body.match(/error[^<]*/i);
      if (errLine) console.log('   ', errLine[0].slice(0, 200));
      bad++;
    }
  } catch (e) {
    console.log(`❌ FETCH ${m}: ${e.message}`);
    bad++;
  }
}
console.log(bad === 0 ? '✅ 全部 ' + modules.length + ' 个模块编译通过' : '共 ' + bad + ' 个失败');
