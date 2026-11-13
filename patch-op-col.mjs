import fs from 'node:fs';

// 1. CrudPage / TreeCrudPage：actionWidth 计算加大 + 操作列 nowrap
for (const p of [
  'E:/文件/编程文件/erp/tiangong-ui/src/components/CrudPage.vue',
  'E:/文件/编程文件/erp/tiangong-ui/src/components/TreeCrudPage.vue'
]) {
  let s = fs.readFileSync(p, 'utf8');
  // actionWidth 基数加大
  s = s.replace(/const actionWidth = computed\(\(\) => (\d+)/, (m, n) => `const actionWidth = computed(() => ${Math.max(parseInt(n), 240)}`);
  // 操作列加 nowrap 类
  s = s.replace(/label="操作" fixed="right"( width="[^"]*")?( :width="actionWidth")?/,
              'label="操作" fixed="right" :width="actionWidth" class-name="op-cell"');
  // 按钮包 op-group
  if (!s.includes('op-group')) {
    s = s.replace(/<template #default="\{ row \}">\s*<el-button v-perm/,
                  '<template #default="{ row }">\n            <div class="op-group">\n            <el-button v-perm');
    // 结尾闭合：操作列 template 结束前补 </div>
    s = s.replace(/(<el-button[^>]*删除\s*<\/el-button>|<el-button[^>]*>\s*删除\s*<\/el-button>)(\s*<\/template>)/,
                  '$1\n            </div>$2');
  }
  fs.writeFileSync(p, s, 'utf8');
  console.log('patched:', p.split('/').pop());
}

// 2. style.css 全局 nowrap 样式
const css = 'E:/文件/编程文件/erp/tiangong-ui/src/style.css';
let c = fs.readFileSync(css, 'utf8');
if (!c.includes('op-cell')) {
  c += `
/* 表格操作列按钮不换行 */
.el-table .op-cell .cell {
  white-space: nowrap;
}
.op-group {
  white-space: nowrap;
  display: inline-block;
}
.op-group .el-button + .el-button {
  margin-left: 8px;
}
`;
  fs.writeFileSync(css, c, 'utf8');
}
console.log('style.css patched:', fs.readFileSync(css, 'utf8').includes('op-cell'));
