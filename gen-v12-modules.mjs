import fs from 'node:fs';
import path from 'node:path';

const base = 'E:/文件/编程文件/erp/tiangong-erp';

// 1. 新模块 pom
const pomFor = (m, desc, extraDeps) => `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>cn.tiangong</groupId>
        <artifactId>tiangong-erp</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>tiangong-module-${m}</artifactId>
    <description>${desc}</description>

    <dependencies>
        <dependency>
            <groupId>cn.tiangong</groupId>
            <artifactId>tiangong-spring-boot-starter-web</artifactId>
        </dependency>
${extraDeps}        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
`;

const fileDeps = `        <dependency>
            <groupId>org.dromara.x-file-storage</groupId>
            <artifactId>x-file-storage-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.dromara.x-file-storage</groupId>
            <artifactId>x-file-storage-local</artifactId>
        </dependency>
`;
const imDeps = `        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.tiangong</groupId>
            <artifactId>tiangong-spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.tiangong</groupId>
            <artifactId>tiangong-module-system</artifactId>
        </dependency>
`;

for (const [m, desc, deps] of [
  ['file', '文件传输系统：分片上传/秒传/下载/文件中心（x-file-storage 嵌入）', fileDeps],
  ['im', 'IM 消息系统：WebSocket 实时单聊（参考 box-im 协议，caojiantao/springboot-websocket 模式）', imDeps]
]) {
  const dir = path.join(base, `tiangong-module-${m}`);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, 'pom.xml'), pomFor(m, desc, deps), 'utf8');
  const src = path.join(dir, 'src', 'main', 'java', 'cn', 'tiangong', 'module', m);
  fs.mkdirSync(src, { recursive: true });
  fs.writeFileSync(path.join(src, 'package-info.java'),
    `/**\n * ${desc}\n */\npackage cn.tiangong.module.${m};\n`, 'utf8');
  console.log('module created:', m);
}

// 2. 根 pom modules
const rootP = path.join(base, 'pom.xml');
let r = fs.readFileSync(rootP, 'utf8');
if (!r.includes('<module>tiangong-module-file</module>')) {
  r = r.replace('        <module>tiangong-server</module>',
    '        <module>tiangong-module-file</module>\n        <module>tiangong-module-im</module>\n        <module>tiangong-server</module>');
  fs.writeFileSync(rootP, r, 'utf8');
}
console.log('root modules wired:', r.includes('tiangong-module-file'));

// 3. BOM：x-file-storage + minio
const bomP = path.join(base, 'tiangong-dependencies', 'pom.xml');
let b = fs.readFileSync(bomP, 'utf8');
if (!b.includes('x-file-storage-core')) {
  b = b.replace(
    /(<dependencyManagement>\s*<dependencies>)/,
    `$1
            <dependency>
                <groupId>org.dromara.x-file-storage</groupId>
                <artifactId>x-file-storage-core</artifactId>
                <version>2.2.1</version>
            </dependency>
            <dependency>
                <groupId>org.dromara.x-file-storage</groupId>
                <artifactId>x-file-storage-local</artifactId>
                <version>2.2.1</version>
            </dependency>
            <dependency>
                <groupId>io.minio</groupId>
                <artifactId>minio</artifactId>
                <version>8.5.17</version>
            </dependency>`);
  fs.writeFileSync(bomP, b, 'utf8');
}
console.log('bom patched:', fs.readFileSync(bomP, 'utf8').includes('x-file-storage-core'));
