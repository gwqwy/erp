import fs from 'node:fs';
let s = fs.readFileSync('gen-v17-backend.mjs', 'utf8');
s = s.split("patch('tiangong-").join("patch('tiangong-erp/tiangong-")
  .split("write('tiangong-module-").join("write('tiangong-erp/tiangong-module-")
  .split("fs.readFileSync('tiangong-module-").join("fs.readFileSync('tiangong-erp/tiangong-module-")
  .split("readFileSync('tiangong-dependencies/pom.xml'").join("readFileSync('tiangong-erp/tiangong-dependencies/pom.xml'");
fs.writeFileSync('gen-v17-backend.mjs', s, 'utf8');
console.log('fixed, check:', s.includes("patch('tiangong-erp/tiangong-dependencies"));
