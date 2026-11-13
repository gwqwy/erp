import fs from 'node:fs';
import path from 'node:path';

const base = 'E:/文件/编程文件/erp/tiangong-erp';
const mods = {
  infra: '平台模块：代码生成/文件/定时任务/API日志（M1 实现）',
  bpm: '平台模块：Flowable 工作流（M1 实现）',
  pur: 'ERP 采购管理（M1 实现）',
  sal: 'ERP 销售管理（M1 实现）',
  stk: 'ERP 库存管理：双账引擎（M1-M2 实现）',
  fin: 'ERP 财务管理（M2-M3 实现）',
  mrp: 'ERP 生产制造（M4 实现）',
  crm: 'ERP 客户关系（M5 实现）',
  hr: 'ERP 人事（M5 实现）',
  rpt: 'ERP 报表与驾驶舱（M5 实现）'
};

const pom = (m, desc) => `<?xml version="1.0" encoding="UTF-8"?>
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
</project>
`;

const pkgInfo = (m, desc) => `/**
 * ${desc}
 */
package cn.tiangong.module.${m};
`;

for (const [m, desc] of Object.entries(mods)) {
  const dir = path.join(base, 'tiangong-module-' + m);
  const pkg = path.join(dir, 'src', 'main', 'java', 'cn', 'tiangong', 'module', m);
  fs.mkdirSync(pkg, { recursive: true });
  fs.writeFileSync(path.join(dir, 'pom.xml'), pom(m, desc), 'utf8');
  fs.writeFileSync(path.join(pkg, 'package-info.java'), pkgInfo(m, desc), 'utf8');
  console.log('generated:', m);
}
console.log('done');
