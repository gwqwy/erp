// 白屏探测器：加载首页并检查 #app 是否有内容；抓 window 错误
const BASE = 'http://localhost:5173';
(async () => {
  // 先探测 employeeApi 模块是否已恢复导出（Vite 转换产物含 employeeApi 字符串）
  const mod = await fetch(BASE + '/src/api/hr.ts').then(r => r.text());
  console.log('hr.ts 含 employeeApi:', mod.includes('employeeApi'));

  const res = await fetch(BASE + '/').then(r => r.text());
  console.log('index.html 长度:', res.length, '含 main.ts 引用:', res.includes('main.ts'));
  console.log('OK - 请浏览器刷新验证');
})();
