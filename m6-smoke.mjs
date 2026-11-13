// 人事深化冒烟：打卡→迟到判定→加班审批→月度汇总→算薪生成→离职审批改员工状态
const BASE = 'http://localhost:48080';

async function api(method, url, body, token) {
  const res = await fetch(BASE + url, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: body ? JSON.stringify(body) : undefined
  });
  const json = await res.json();
  if (json.code !== 0) throw new Error(`${url} -> code=${json.code} msg=${json.msg}`);
  return json.data;
}

const login = await api('POST', '/admin-api/system/auth/login', { username: 'admin', password: 'admin123' });
const t = login.accessToken;
console.log('1. 登录 OK');

// 员工 + 基础工资 8800
const empId = await api('POST', '/admin-api/hr/employee/create', {
  name: '王考勤', deptId: 3, post: '装配工', baseSalary: 8800
}, t);
console.log('2. 员工创建 OK（基础工资 8800）:', empId);

// 考勤：打卡（今天第一次打卡）
await api('POST', `/admin-api/hr/attendance/check-in?employeeId=${empId}`, null, t);
const page = await api('GET', `/admin-api/hr/attendance/page?pageNo=1&pageSize=5&employeeId=${empId}`, null, t);
const rec = page.list[0];
console.log(`3. 上班打卡 OK: checkIn=${rec.checkIn}, status=${rec.status} (1=迟到若晚于9点)`);
await api('POST', `/admin-api/hr/attendance/check-out?employeeId=${empId}`, null, t);
console.log('4. 下班打卡 OK');

// 补录一条缺勤（昨天）
const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10);
await api('POST', `/admin-api/hr/attendance/save?employeeId=${empId}&attDate=${yesterday}&status=3&remark=未出勤`, null, t);
console.log('5. 补录缺勤 1 天 OK');

// 加班 8 小时：先配置审批流（幂等），再创建+提交+审批
const cfgPage = await api('GET', '/admin-api/bpm/config/page?pageNo=1&pageSize=50&bizType=hr_overtime', null, t);
if (!cfgPage.list.length) {
  await api('POST', `/admin-api/bpm/config/create?bizType=hr_overtime&level=1&approverUserId=${login.userId}`, null, t);
}
const otId2 = await api('POST', `/admin-api/hr/overtime/create?employeeId=${empId}&hours=8&reason=赶工M6`, null, t);
await api('POST', `/admin-api/hr/overtime/submit-approval?id=${otId2}`, null, t);
const myTasks = await api('GET', '/admin-api/bpm/my-tasks', null, t);
const otTask = myTasks.find(x => x.bizType === 'hr_overtime' && String(x.bizId) === String(otId2));
await api('POST', '/admin-api/bpm/approve', { taskId: otTask.id, comment: 'OK' }, t);
const otPage = await api('GET', `/admin-api/hr/overtime/page?pageNo=1&pageSize=5&employeeId=${empId}`, null, t);
const otDone = otPage.list.find(o => String(o.id) === String(otId2));
console.log(`6. 加班走审批流 OK: 状态=${otDone.status} (30=通过)`);

// 月度汇总
const summary = await api('GET', `/admin-api/hr/attendance/summary?period=2026-11`, null, t);
const mine = summary.find(s => String(s.employeeId) === String(empId));
console.log(`7. 月度汇总 OK: 出勤=${mine.presentDays} 迟到=${mine.lateDays} 缺勤=${mine.absentDays}`);

// 奖惩：奖励 500
await api('POST', `/admin-api/hr/reward-penalty/create?employeeId=${empId}&type=reward&amount=500&reason=合理化建议奖`, null, t);
console.log('8. 奖励 500 登记 OK');

// 算薪：base 8800 + OT 8h×(8800/21.75/8×1.5=75.86)=606.9 + 500 - 缺勤1天×404.6 → net
const calc = await api('GET', `/admin-api/hr/payroll/calculate?period=2026-11&employeeId=${empId}`, null, t);
console.log(`9. 算薪试算 OK: 应发=${calc.gross} 加班费=${calc.overtimeAmount} 缺勤扣=${calc.absenceDeduction} 奖励=${calc.rewardAmount} 实发=${calc.net}`);
const salId = await api('POST', `/admin-api/hr/payroll/generate?period=2026-11&employeeId=${empId}&remark=M6冒烟`, null, t);
console.log(`10. 薪资生成 OK: hr_salary id=${salId}`);

// 离职：提交审批 → 通过 → 员工状态变离职
const cfgRz = await api('GET', '/admin-api/bpm/config/page?pageNo=1&pageSize=50&bizType=hr_resign', null, t);
if (!cfgRz.list.length) {
  await api('POST', `/admin-api/bpm/config/create?bizType=hr_resign&level=1&approverUserId=${login.userId}`, null, t);
}
const rzId = await api('POST', `/admin-api/hr/resignation/create?employeeId=${empId}&planLeaveDate=2026-11-30&reason=M6冒烟`, null, t);
await api('POST', `/admin-api/hr/resignation/submit-approval?id=${rzId}`, null, t);
const rzTasks = await api('GET', '/admin-api/bpm/my-tasks', null, t);
const rzTask = rzTasks.find(x => x.bizType === 'hr_resign' && String(x.bizId) === String(rzId));
await api('POST', '/admin-api/bpm/approve', { taskId: rzTask.id, comment: '同意离职' }, t);
const emp = await api('GET', `/admin-api/hr/employee/page?pageNo=1&pageSize=20`, null, t);
const empAfter = emp.list.find(e => String(e.id) === String(empId));
console.log(`11. 离职审批 OK: 员工状态=${empAfter.status} (1=离职), 离职日期=${empAfter.leaveDate}`);

console.log('\n✅ 人事深化冒烟测试全部通过');
