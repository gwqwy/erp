// M9 冒烟：文件传输（分片/秒传/下载/直传） + IM（REST/WS/历史）
const BASE = 'http://localhost:48080';
const UI = 'http://localhost:5173';
const ok = (name, extra) => console.log('✅', name, extra !== undefined ? '→ ' + extra : '');
const fail = (name, e) => { console.log('❌', name, e?.message || e); process.exit(1); };

async function api(method, url, token, body, raw) {
  const res = await fetch(BASE + url, {
    method,
    headers: {
      Authorization: 'Bearer ' + token,
      ...(raw ? body : body ? { 'Content-Type': 'application/json' } : {})
    },
    body: raw ? body : body ? JSON.stringify(body) : undefined
  });
  const text = await res.text();
  let json;
  try { json = JSON.parse(text); } catch { throw new Error('HTTP ' + res.status + ' non-json: ' + text.slice(0, 120)); }
  if (json.code !== 0) throw new Error('code=' + json.code + ' msg=' + json.msg);
  return json.data;
}

try {
  // ---------- 登录 ----------
  const login = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json());
  const t = login.data.accessToken;
  ok('登录', 'admin');

  // ---------- 用户列表（确保有第二个用户） ----------
  const users = await api('GET', '/admin-api/system/user/page?pageNo=1&pageSize=20', t);
  let peer = users.list.find(u => u.username === 'm9peer');
  if (!peer) {
    const pid = await api('POST', '/admin-api/system/user/create', t,
      { username: 'm9peer', password: 'admin123', nickname: '冒烟同事', deptId: 3 });
    peer = { id: pid, username: 'm9peer', nickname: '冒烟同事' };
    ok('创建冒烟同事用户', 'id=' + pid);
  }
  ok('用户列表', users.list.length + ' 人，peer=' + peer.id + ' ' + (peer.nickname || peer.username));

  // ---------- 文件：分片上传 12MB（3 片 5/5/2） ----------
  const TOTAL = 12 * 1024 * 1024;
  const buf = Buffer.alloc(TOTAL);
  // 内容每次运行唯一（开头写入时间戳），避免命中上轮秒传
  buf.write('RUN-' + Date.now(), 0);
  for (let i = 32; i < TOTAL; i += 4096) buf.write('TIANGONG' + (i % 997), i % (TOTAL - 16));
  // md5
  const { createHash } = await import('node:crypto');
  const md5 = createHash('md5').update(buf).digest('hex');
  const fileName = 'm9-smoke-' + Date.now() + '.bin';

  const init = await api('POST', `/admin-api/file/chunk/init?fileName=${encodeURIComponent(fileName)}&md5=${md5}&totalChunks=3&totalSize=${TOTAL}`, t, null);
  ok('分片 init', 'mode=' + init.mode);
  if (init.mode !== 'new') throw new Error('期望 new，实际 ' + init.mode);
  const uploadId = init.uploadIdOrName;

  const CHUNK = 5 * 1024 * 1024;
  for (let i = 0; i < 3; i++) {
    const slice = buf.subarray(i * CHUNK, Math.min((i + 1) * CHUNK, TOTAL));
    const form = new FormData();
    form.append('file', new Blob([slice]));
    const r = await fetch(BASE + `/admin-api/file/chunk/upload?uploadId=${uploadId}&chunkIndex=${i}`, {
      method: 'POST', headers: { Authorization: 'Bearer ' + t }, body: form
    }).then(r => r.json());
    if (r.code !== 0) throw new Error('chunk ' + i + ' 失败: ' + r.msg);
  }
  ok('分片上传 ×3');

  const merged = await api('POST', `/admin-api/file/chunk/merge?uploadId=${uploadId}&fileName=${encodeURIComponent(fileName)}&md5=${md5}&totalChunks=3`, t, null);
  ok('合并入库', 'id=' + merged.id + ' size=' + merged.fileSize);
  if (Number(merged.fileSize) !== TOTAL) throw new Error('合并大小不符: ' + merged.fileSize);

  // ---------- 秒传：同 md5 再次 init ----------
  const init2 = await api('POST', `/admin-api/file/chunk/init?fileName=${encodeURIComponent(fileName)}&md5=${md5}&totalChunks=3&totalSize=${TOTAL}`, t, null);
  ok('秒传 init', 'mode=' + init2.mode);
  if (init2.mode !== 'instant') throw new Error('秒传失败，mode=' + init2.mode);

  // check-md5 接口
  const hit = await api('GET', `/admin-api/file/check-md5?md5=${md5}`, t);
  ok('check-md5', 'id=' + hit.id);

  // ---------- 下载比对 ----------
  const dl = await fetch(BASE + `/admin-api/file/download?id=${merged.id}`, {
    headers: { Authorization: 'Bearer ' + t }
  });
  const dlBuf = Buffer.from(await dl.arrayBuffer());
  ok('下载', dl.status + ' ' + dlBuf.length + ' bytes');
  if (dlBuf.length !== TOTAL) throw new Error('下载大小不符 ' + dlBuf.length);
  const dlMd5 = createHash('md5').update(dlBuf).digest('hex');
  if (dlMd5 !== md5) throw new Error('下载内容 md5 不符');
  ok('下载内容比对', 'md5 一致');

  // ---------- 直传端点 ----------
  const small = Buffer.from('hello tiangong direct upload');
  const form2 = new FormData();
  form2.append('file', new Blob([small]), 'direct.txt');
  const up = await fetch(BASE + '/admin-api/file/upload?bizType=smoke', {
    method: 'POST', headers: { Authorization: 'Bearer ' + t }, body: form2
  }).then(r => r.json());
  if (up.code !== 0) throw new Error('直传失败: ' + up.msg);
  ok('直传 /upload', 'id=' + up.data.id + ' name=' + up.data.fileName);

  // ---------- 文件列表 / 删除 ----------
  const fl = await api('GET', `/admin-api/file/list?fileName=${encodeURIComponent('direct.txt')}`, t);
  ok('文件列表', fl.length + ' 条');
  await api('DELETE', `/admin-api/file/delete?id=${up.data.id}`, t, null);
  ok('删除直传记录');

  // ---------- IM：REST 发送 → 会话 → 历史 ----------
  const info = await api('GET', '/admin-api/system/auth/get-permission-info', t);
  const myId = info.user.id;
  const sent = await api('POST', `/admin-api/im/send?receiverId=${peer.id}&content=${encodeURIComponent('m9 REST 你好')}`, t, null);
  ok('IM REST 发送', 'msgId=' + sent.id);
  const sessions = await api('GET', '/admin-api/im/sessions', t);
  const sKey = [BigInt(myId), BigInt(peer.id)].sort((a, b) => (a < b ? -1 : a > b ? 1 : 0)).join('_');
  const sess = sessions.find(s => s.sessionKey === sKey);
  if (!sess) throw new Error('会话未找到: ' + sKey + '，现有: ' + sessions.map(s => s.sessionKey).join(','));
  ok('IM 会话列表', 'sessionKey=' + sess.sessionKey + ' last=' + sess.lastContent);
  const hist = await api('GET', `/admin-api/im/history?sessionKey=${sKey}`, t);
  if (!hist.some(m => m.content === 'm9 REST 你好')) throw new Error('历史消息缺 REST 消息');
  ok('IM 历史', hist.length + ' 条');

  // ---------- IM：WebSocket（admin 连接 + 发送；peer 连接收推送） ----------
  let peerToken = null;
  const peerLogin = await fetch(BASE + '/admin-api/system/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: peer.username, password: 'admin123' })
  }).then(r => r.json()).catch(() => null);
  if (peerLogin?.code === 0) peerToken = peerLogin.data.accessToken;

  const received = [];
  let wsPeer = null;
  if (peerToken) {
    await new Promise((resolve) => {
      wsPeer = new WebSocket(`ws://127.0.0.1:48080/ws/im/${peerToken}`);
      wsPeer.onopen = resolve;
      wsPeer.onerror = resolve;
      setTimeout(resolve, 4000);
    });
    wsPeer.onmessage = e => { try { const j = JSON.parse(e.data); if (j.type === 'chat') received.push(j); } catch { } };
    ok('peer WS 已连接');
  } else {
    console.log('⚠️ peer 无法登录（跳过 WS 推送验证）');
  }

  const wsAdmin = await new Promise((resolve, reject) => {
    const ws = new WebSocket(`ws://127.0.0.1:48080/ws/im/${t}`);
    const to = setTimeout(() => reject(new Error('admin WS 连接超时')), 5000);
    ws.onopen = () => { clearTimeout(to); resolve(ws); };
    ws.onerror = () => { clearTimeout(to); reject(new Error('admin WS 连接失败')); };
  });
  ok('admin WS 已连接');
  // 先等服务端 {"type":"open"} 报文（证明 onOpen 走通）
  await new Promise(resolve => {
    wsAdmin.onmessage = e => { if (String(e.data).includes('open')) resolve(); };
    setTimeout(resolve, 4000);
  });
  // 再验证心跳
  const pingPong = await new Promise(resolve => {
    wsAdmin.onmessage = e => resolve(e.data === 'pong' ? 'pong' : 'other:' + e.data);
    wsAdmin.send('ping');
    setTimeout(() => resolve('timeout'), 4000);
  });
  if (pingPong !== 'pong') throw new Error('心跳失败: ' + pingPong);
  ok('WS 心跳 ping→pong');

  wsAdmin.send(JSON.stringify({ type: 'chat', receiverId: String(peer.id), content: 'm9 WS 你好' }));
  await new Promise(r => setTimeout(r, 1200));
  if (peerToken) {
    if (!received.some(m => m.content === 'm9 WS 你好')) throw new Error('WS 推送未到达 peer');
    ok('WS 实时推送到 peer', '内容一致');
    wsPeer.close();
  }
  wsAdmin.close();

  const hist2 = await api('GET', `/admin-api/im/history?sessionKey=${sKey}`, t);
  if (!hist2.some(m => m.content === 'm9 WS 你好')) throw new Error('历史消息缺 WS 消息');
  ok('WS 消息已落库', hist2.length + ' 条');

  // ---------- 前端页面编译探测 ----------
  for (const p of ['src/views/im/index.vue', 'src/views/more/file-center/index.vue',
    'src/views/fin/period-close/index.vue', 'src/views/more/form-data/index.vue',
    'src/views/fin/invoice/index.vue', 'src/api/file.ts', 'src/api/im.ts']) {
    const res = await fetch(UI + '/' + p);
    if (res.status !== 200) throw new Error('页面编译失败 ' + p + ' → ' + res.status);
  }
  ok('前端 7 个新文件 vite 编译 200');

  console.log('\n🎉 M9 冒烟全部通过');
} catch (e) {
  fail('M9', e);
}
