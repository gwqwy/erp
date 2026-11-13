import fs from 'node:fs';
import path from 'node:path';
const root = 'E:/文件/编程文件/erp/tiangong-erp';
const out = [];
function walk(d) {
  for (const f of fs.readdirSync(d)) {
    const fp = path.join(d, f);
    const st = fs.statSync(fp);
    if (st.isDirectory()) walk(fp);
    else if (f.endsWith('Controller.java')) out.push(fp);
  }
}
walk(root);
const re = /RequestMapping\(\s*"([^"]+)"/;
const pat = /.*/i;
for (const fp of out) {
  const s = fs.readFileSync(fp, 'utf8');
  const m = s.match(re);
  const base = m ? m[1] : '(no-mapping)';
  if (pat.test(base) || pat.test(path.basename(fp))) {
    const methods = [...s.matchAll(/@(Get|Post|Put|Delete)Mapping(?:\("([^"]*)"\))?/g)]
      .map(x => (x[2] ? x[1].toLowerCase() + ':' + x[2] : x[1].toLowerCase())).join(', ');
    console.log(base, '<=', path.basename(fp));
    console.log('   ', methods);
  }
}
