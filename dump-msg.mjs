import fs from 'node:fs';
import path from 'node:path';
const out = [];
function walk(d, o) { for (const f of fs.readdirSync(d)) { const fp = path.join(d, f); const st = fs.statSync(fp); if (st.isDirectory()) walk(fp, o); else o.push(fp); } }
walk('tiangong-erp/tiangong-module-system/src/main/java', out);
for (const f of out) {
  const b = path.basename(f);
  if (/Message.*DO\.java$/.test(b) || /MessageDO\.java$/.test(b)) {
    const s = fs.readFileSync(f, 'utf8');
    console.log('==', b);
    console.log([...s.matchAll(/private (\w+) (\w+);/g)].map(x => x[1] + ' ' + x[2]).join(', '));
    console.log((s.match(/@TableName\("[^"]+"\)/) || [''])[0]);
  }
}
// infra pom deps
const pom = fs.readFileSync('tiangong-erp/tiangong-module-infra/pom.xml', 'utf8');
console.log('infra deps:', [...pom.matchAll(/<artifactId>([\w-]+)<\/artifactId>/g)].map(m => m[1]).join(', '));
// stk pom deps (for job)
const poms = fs.readFileSync('tiangong-erp/tiangong-module-stk/pom.xml', 'utf8');
console.log('stk deps:', [...poms.matchAll(/<artifactId>([\w-]+)<\/artifactId>/g)].map(m => m[1]).join(', '));
// rpt pom deps
const pomr = fs.readFileSync('tiangong-erp/tiangong-module-rpt/pom.xml', 'utf8');
console.log('rpt deps:', [...pomr.matchAll(/<artifactId>([\w-]+)<\/artifactId>/g)].map(m => m[1]).join(', '));
// MessageController send signature
const mc = out.find(f => f.endsWith('MessageController.java'));
if (mc) {
  const s = fs.readFileSync(mc, 'utf8');
  const i = s.indexOf('/send');
  console.log('---- MessageController /send ----');
  console.log(s.slice(Math.max(0, i - 100), i + 500));
}
// system_message table DDL from V11
const v11 = fs.readFileSync('tiangong-erp/tiangong-server/src/main/resources/db/migration/V11__full_completion.sql', 'utf8');
const i2 = v11.indexOf('system_message');
if (i2 >= 0) console.log('---- V11 system_message DDL ----\n' + v11.slice(i2, i2 + 900));
