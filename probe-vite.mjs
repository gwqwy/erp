// 逐个探测前端模块，找出编译失败（非 200）的那个
const base = 'http://localhost:5173/src/';
const modules = [
  'main.ts',
  'router/views.ts',
  'router/index.ts',
  'stores/menu.ts',
  'stores/user.ts',
  'views/hr/apply-page.vue',
  'views/hr/attendance/index.vue',
  'views/hr/attendance-summary/index.vue',
  'views/hr/overtime/index.vue',
  'views/hr/trip/index.vue',
  'views/hr/resignation/index.vue',
  'views/hr/reward-penalty/index.vue',
  'views/hr/contract/index.vue',
  'views/hr/payroll/index.vue',
  'views/hr/employee/index.vue',
  'views/bpm/my-tasks/index.vue',
  'views/more/dynamic-form/index.vue',
  'views/more/quality-check/index.vue',
  'views/more/batch-trace/index.vue',
  'views/report/balance/index.vue',
  'views/report/profit/index.vue',
  'views/crm/lead/index.vue',
  'views/crm/business/index.vue',
  'views/fin/receipt/index.vue',
  'views/mrp/plan/index.vue'
];

for (const m of modules) {
  try {
    const res = await fetch(base + m, { headers: { Accept: '*/*' } });
    if (res.status !== 200) {
      const body = await res.text();
      console.log(`❌ ${res.status} ${m}`);
      console.log('   ', body.slice(0, 300).replace(/\n/g, '\n    '));
    } else {
      console.log(`✅ 200 ${m}`);
    }
  } catch (e) {
    console.log(`❌ FETCH FAIL ${m}: ${e.message}`);
  }
}
