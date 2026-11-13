import fs from 'node:fs';
import path from 'node:path';
function walk(d, out) { for (const f of fs.readdirSync(d)) { const fp = path.join(d, f); const st = fs.statSync(fp); if (st.isDirectory()) walk(fp, out); else out.push(fp); } }
const out = [];
for (const m of ['sal', 'pur', 'fin', 'stk']) walk(`tiangong-erp/tiangong-module-${m}/src/main/java`, out);
for (const f of out) {
  if (!f.endsWith('DO.java')) continue;
  const s = fs.readFileSync(f, 'utf8');
  const tn = (s.match(/@TableName\("([^"]+)"\)/) || [])[1];
  console.log((tn || '?'), '<=', path.basename(f));
}
