import { execSync } from 'node:child_process';

const deps = [
  ['org.springframework.boot:spring-boot-starter-websocket:3.4.5', 'WebSocket starter (随 Boot 3.4.5)'],
  ['io.minio:minio:8.5.17', 'MinIO 客户端'],
  ['org.dromara.x-file-storage:x-file-storage-core:2.2.1', 'X File Storage 文件存储中间件'],
  ['org.yeauty:netty-websocket-spring-boot-starter:2.1.0', 'Netty WebSocket starter']
];

for (const [coord, desc] of deps) {
  try {
    execSync(`mvn -B -q dependency:get -Dartifact=${coord} -Dtransitive=false`, { stdio: 'pipe' });
    console.log('✅ 可解析:', desc, '(' + coord + ')');
  } catch (e) {
    console.log('❌ 不可解析:', desc, '(' + coord + ')');
  }
}
