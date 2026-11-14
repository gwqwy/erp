import fs from 'node:fs';
import path from 'node:path';

const UI = 'E:/文件/编程文件/erp/tiangong-ui/src';

function write(rel, content) {
  const fp = path.join(UI, rel);
  fs.mkdirSync(path.dirname(fp), { recursive: true });
  fs.writeFileSync(fp, content, 'utf8');
  console.log('write:', rel);
}

function append(rel, block, anchor) {
  const fp = path.join(UI, rel);
  let s = fs.readFileSync(fp, 'utf8');
  if (s.includes(anchor)) { console.log('skip:', rel, anchor); return; }
  fs.writeFileSync(fp, s.trimEnd() + '\n' + block, 'utf8');
  console.log('append:', rel, anchor);
}

// ================= api =================

append('api/rpt.ts', `
// ========== V16：收发存汇总 / 供应商对账单 ==========
export const stockFlowApi = {
  summary: (params: { from: string; to: string }) => get<any[]>('/admin-api/rpt/stock-flow', params)
}

export const supplierStatementApi = {
  detail: (params: { partnerId: string; from: string; to: string }) =>
    get<any>('/admin-api/rpt/supplier-statement', params)
}
`, 'stockFlowApi');

append('api/system.ts', `
// ========== V16：站内消息（顶栏铃铛 / 消息中心） ==========
export const messageApi = {
  myUnread: () => get<any[]>('/admin-api/system/message/my-unread'),
  myUnreadCount: () => get<number>('/admin-api/system/message/my-unread-count'),
  read: (id: string) => post('/admin-api/system/message/read', null, { id }),
  readAll: () => post('/admin-api/system/message/read-all')
}
`, 'messageApi');

// ================= 收发存汇总页 =================

write('views/report/stock-flow/index.vue', `<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;gap:8px;align-items:center">
        <span>收发存汇总（期初 = 期末 − 收 + 发）</span>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                        start-placeholder="开始" end-placeholder="结束" style="width:260px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="exportCsv">导出 CSV</el-button>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" border stripe show-summary :summary-method="summary">
      <el-table-column prop="productNo" label="商品编码" width="140" />
      <el-table-column prop="productName" label="商品名称" min-width="180" />
      <el-table-column prop="beginQty" label="期初数量" width="120" align="right" />
      <el-table-column prop="inQty" label="本期入库" width="120" align="right" />
      <el-table-column prop="outQty" label="本期出库" width="120" align="right" />
      <el-table-column prop="endQty" label="期末数量" width="120" align="right" />
      <el-table-column prop="beginAmount" label="期初金额" width="130" align="right" />
      <el-table-column prop="inAmount" label="入库金额" width="130" align="right" />
      <el-table-column prop="outAmount" label="出库金额" width="130" align="right" />
      <el-table-column prop="endAmount" label="期末金额" width="130" align="right" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { stockFlowApi } from '@/api/rpt'

const rows = ref<any[]>([])
const loading = ref(false)
const range = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const params: any = {}
    if (range.value) { params.from = range.value[0]; params.to = range.value[1] }
    else {
      const now = new Date()
      params.from = now.getFullYear() + '-01-01'
      params.to = now.toISOString().slice(0, 10)
    }
    rows.value = await stockFlowApi.summary(params)
  } finally {
    loading.value = false
  }
}

function sum(prop: string) {
  return rows.value.reduce((s, r) => s + Number(r[prop] || 0), 0).toFixed(2)
}

function summary({ columns }: any) {
  return columns.map((col: any, i: number) => {
    if (i === 0) return '合计'
    if (['beginQty', 'inQty', 'outQty', 'endQty', 'beginAmount', 'inAmount', 'outAmount', 'endAmount'].includes(col.property)) {
      return sum(col.property)
    }
    return ''
  })
}

function exportCsv() {
  const header = '商品编码,商品名称,期初数量,本期入库,本期出库,期末数量,期初金额,入库金额,出库金额,期末金额'
  const body = rows.value.map(r =>
    [r.productNo, r.productName, r.beginQty, r.inQty, r.outQty, r.endQty,
      r.beginAmount, r.inAmount, r.outAmount, r.endAmount].join(',')).join('\n')
  const blob = new Blob(['\\uFEFF' + header + '\n' + body], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '收发存汇总.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}

onMounted(load)
</script>
`)

// ================= 供应商对账单页 =================

write('views/report/supplier-statement/index.vue', `<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
        <span>供应商对账单</span>
        <el-select v-model="partnerId" filterable placeholder="选择供应商" style="width:220px">
          <el-option v-for="p in partners" :key="p.value" :label="p.label" :value="p.value" />
        </el-select>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                        start-placeholder="开始" end-placeholder="结束" style="width:260px" />
        <el-button type="primary" :disabled="!partnerId" @click="load">查询</el-button>
        <el-button :disabled="!partnerId" @click="print">打印对账单</el-button>
      </div>
    </template>

    <div v-if="data">
      <div style="display:flex;gap:24px;margin-bottom:12px">
        <el-tag type="info">期初余额：{{ data.opening }}</el-tag>
        <el-tag type="warning">本期收货：{{ data.totalDebit }}</el-tag>
        <el-tag type="success">本期付款：{{ data.totalCredit }}</el-tag>
        <el-tag type="danger">期末应付：{{ data.closing }}</el-tag>
      </div>
      <el-table :data="data.rows" v-loading="loading" border stripe>
        <el-table-column prop="date" label="日期" width="130" />
        <el-table-column prop="kind" label="业务类型" width="110" />
        <el-table-column prop="no" label="单号" width="180" />
        <el-table-column label="收货(借)" width="140" align="right">
          <template #default="{ row }">{{ Number(row.debit) !== 0 ? row.debit : '' }}</template>
        </el-table-column>
        <el-table-column label="付款(贷)" width="140" align="right">
          <template #default="{ row }">{{ Number(row.credit) !== 0 ? row.credit : '' }}</template>
        </el-table-column>
        <el-table-column prop="balance" label="余额" align="right" />
      </el-table>
      <el-empty v-if="data && (!data.rows || !data.rows.length) && !loading" description="该期间无往来明细" :image-size="70" />
    </div>
    <el-empty v-else description="选择供应商与期间后查询" :image-size="80" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { supplierStatementApi } from '@/api/rpt'
import { partnerApi } from '@/api/bas'

const partners = ref<any[]>([])
const partnerId = ref('')
const range = ref<any>(null)
const data = ref<any>(null)
const loading = ref(false)

onMounted(async () => {
  const page = await partnerApi.page({ pageNo: 1, pageSize: 200 })
  partners.value = (page.list || []).filter((p: any) => p.isSupplier)
    .map((p: any) => ({ label: p.name, value: p.id }))
})

async function load() {
  if (!partnerId.value) return
  loading.value = true
  try {
    const params: any = { partnerId: partnerId.value }
    if (range.value) { params.from = range.value[0]; params.to = range.value[1] }
    else {
      const now = new Date()
      params.from = now.getFullYear() + '-01-01'
      params.to = now.toISOString().slice(0, 10)
    }
    data.value = await supplierStatementApi.detail(params)
  } finally {
    loading.value = false
  }
}

function print() {
  window.print()
}
</script>
`)

// ================= 消息中心页 =================

write('views/system/message/index.vue', `<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>消息中心</span>
        <el-button type="primary" size="small" :disabled="!unread.length" @click="markAll">全部标记已读</el-button>
      </div>
    </template>

    <el-tabs v-model="tab">
      <el-tab-pane :label="'未读 (' + unread.length + ')'" name="unread" />
      <el-tab-pane label="已读" name="read" />
    </el-tabs>

    <el-table v-if="tab === 'unread'" :data="unread" v-loading="loading" border stripe>
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="creator" label="发送人" width="110" />
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" class-name="op-cell">
        <template #default="{ row }">
          <div class="op-group">
            <el-button link type="primary" @click="markRead(row)">标记已读</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-table v-else :data="readRows" v-loading="loading" border stripe>
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="creator" label="发送人" width="110" />
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !(tab === 'unread' ? unread.length : readRows.length)" description="暂无消息" :image-size="80" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { messageApi } from '@/api/system'

const tab = ref('unread')
const loading = ref(false)
const unread = ref<any[]>([])
const readRows = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    unread.value = await messageApi.myUnread()
    // 已读列表：全量拉取由后端 my-unread 之外无接口，这里从 unread 全量接口不行——
    // 用 my-unread 只给未读；已读走简单过滤方案：请求全部消息（复用 list 场景有限，此处取未读 + 本地已读缓存）
    readRows.value = readCache
  } finally {
    loading.value = false
  }
}

// 已读消息本会话缓存（进入页面时从未读标为已读的进入此列表）
const readCache: any[] = []

async function markRead(row: any) {
  await messageApi.read(row.id)
  readCache.unshift({ ...row, readFlag: true })
  unread.value = unread.value.filter((u: any) => u.id !== row.id)
}

async function markAll() {
  await messageApi.readAll()
  readCache.unshift(...unread.value.map((u: any) => ({ ...u, readFlag: true })))
  unread.value = []
  ElMessage.success('已全部标记已读')
}

function fmtTime(t: string) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}

onMounted(load)
</script>
`)

// ================= 顶栏铃铛 =================

const layoutPath = path.join(UI, 'layout/index.vue');
let L = fs.readFileSync(layoutPath, 'utf8');
if (!L.includes('messageApi')) {
  L = L.replace(
    `        <el-dropdown @command="onCommand">
          <span class="user-entry">`,
    `        <el-dropdown trigger="click" @command="onBellCommand" style="margin-right:16px">
          <span class="user-entry" style="gap:2px">
            <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
              <el-icon :size="18"><Bell /></el-icon>
            </el-badge>
          </span>
          <template #dropdown>
            <el-dropdown-menu style="min-width:280px">
              <el-dropdown-item v-for="msg in unreadTop" :key="msg.id" :command="'read:' + msg.id" divided>
                <div style="max-width:280px">
                  <div style="font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ msg.title }}</div>
                  <div style="font-size:12px;color:#909399;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ msg.content }}</div>
                </div>
              </el-dropdown-item>
              <el-dropdown-item v-if="!unreadTop.length" disabled divided>暂无未读消息</el-dropdown-item>
              <el-dropdown-item command="readall" divided>全部标记已读</el-dropdown-item>
              <el-dropdown-item command="center" divided>进入消息中心</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown @command="onCommand">
          <span class="user-entry">`
  );
  L = L.replace(
    "import { useMenuStore } from '@/stores/menu'",
    `import { useMenuStore } from '@/stores/menu'
import { messageApi } from '@/api/system'
import { Bell } from '@element-plus/icons-vue'`
  );
  // 注入逻辑：在 onCommand 函数定义前插入铃铛逻辑（锚定 script 内已有函数名）
  L = L.replace(
    /const activeMenu = computed\(\(\) => route\.path\)/,
    `const activeMenu = computed(() => route.path)

// ===== 顶栏消息铃铛：30s 轮询未读数 =====
const unreadCount = ref(0)
const unreadTop = ref<any[]>([])

async function refreshBell() {
  try {
    unreadCount.value = Number(await messageApi.myUnreadCount()) || 0
    unreadTop.value = (await messageApi.myUnread()).slice(0, 5)
  } catch {
    // 静默：轮询失败不打扰用户
  }
}

async function onBellCommand(cmd: string) {
  if (cmd === 'readall') {
    await messageApi.readAll()
  } else if (cmd.startsWith('read:')) {
    await messageApi.read(cmd.slice(5))
  } else if (cmd === 'center') {
    router.push('/system/message')
    return
  }
  refreshBell()
}

let bellTimer: any = null
onMounted(() => {
  refreshBell()
  bellTimer = setInterval(refreshBell, 30000)
})
onBeforeUnmount(() => {
  if (bellTimer) clearInterval(bellTimer)
})`
  );
  L = L.replace(
    "import { computed } from 'vue'",
    "import { computed, onBeforeUnmount, onMounted, ref } from 'vue'"
  );
  fs.writeFileSync(layoutPath, L, 'utf8');
}
console.log('layout bell wired:', L.includes('refreshBell'));

// ================= 导入模板下载 =================

const importPath = path.join(UI, 'views/infra/import/index.vue');
let I = fs.readFileSync(importPath, 'utf8');
if (!I.includes('downloadTemplate')) {
  I = I.replace(
    `    <el-upload :show-file-list="false" :auto-upload="false" accept=".csv,.txt" :on-change="onPick">
      <el-button type="primary" :loading="uploading">选择 CSV 并导入</el-button>
    </el-upload>`,
    `    <div style="display:flex;gap:10px">
      <el-upload :show-file-list="false" :auto-upload="false" accept=".csv,.txt" :on-change="onPick">
        <el-button type="primary" :loading="uploading">选择 CSV 并导入</el-button>
      </el-upload>
      <el-button @click="downloadTemplate">下载{{ tabName }}模板</el-button>
    </div>`
  );
  I = I.replace(
    `const tab = ref('products')
const uploading = ref(false)
const result = ref<any>(null)`,
    `const tab = ref('products')
const uploading = ref(false)
const result = ref<any>(null)

const tabName = computed(() => ({ products: '商品', partners: '往来单位', 'stock-openings': '库存期初' }[tab.value] || ''))

const TEMPLATES: Record<string, string> = {
  products: '编码,名称,规格,采购价,销售价,安全库存\\nP001,示例商品,规格A,10,20,5\\nP002,示例商品B,,8,15,3',
  partners: '名称,类型,联系人,电话,地址,备注\\n示例客户,customer,张三,13800000000,北京市,',
  'stock-openings': '仓库编码,商品编码,数量,成本价\\n主仓库,P001,100,10'
}

function downloadTemplate() {
  const csv = TEMPLATES[tab.value] || ''
  const blob = new Blob(['\\uFEFF' + csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = (tabName.value || '导入') + '导入模板.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}`
  );
  I = I.replace("import { ref } from 'vue'", "import { computed, ref } from 'vue'");
  fs.writeFileSync(importPath, I, 'utf8');
}
console.log('import template button:', I.includes('downloadTemplate'));

// ================= 工作台待办卡 =================

const dashPath = path.join(UI, 'views/dashboard/index.vue');
let D = fs.readFileSync(dashPath, 'utf8');
if (!D.includes('todoCards')) {
  D = D.replace(
    `    <!-- 趋势图 -->`,
    `    <!-- 待办提醒 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="8" v-for="card in todoCards" :key="card.label">
        <el-card shadow="hover" class="todo-card" @click="card.go">
          <div class="todo-row">
            <span class="todo-label">{{ card.label }}</span>
            <el-badge :value="card.value" :type="card.value > 0 ? 'danger' : 'info'" :max="99" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 -->`
  );
  D = D.replace(
    "import { dashboardApi } from '@/api/rpt'",
    `import { dashboardApi } from '@/api/rpt'
import { bpmApi } from '@/api/bpm'
import { messageApi } from '@/api/system'
import { alertScanApi } from '@/api/stock'
import { useRouter } from 'vue-router'`
  );
  D = D.replace(
    "const summary = ref<any>(null)",
    `const router = useRouter()
const summary = ref<any>(null)
const todoCards = ref<Array<{ label: string; value: number; go: () => void }>>([
  { label: '待办审批任务', value: 0, go: () => router.push('/bpm/my-tasks') },
  { label: '未读站内消息', value: 0, go: () => router.push('/system/message') },
  { label: '低库存预警商品', value: 0, go: () => router.push('/stk/alert') }
])

async function loadTodos() {
  try {
    const tasks = await bpmApi.myTasks()
    todoCards.value[0].value = Number(tasks?.total ?? (tasks?.list?.length ?? 0)) || 0
  } catch { /* 审批接口失败不阻塞 */ }
  try {
    todoCards.value[1].value = Number(await messageApi.myUnreadCount()) || 0
  } catch { /* 消息接口失败不阻塞 */ }
  try {
    const lows = await alertScanApi.lowList()
    todoCards.value[2].value = (lows || []).length
  } catch { /* 预警接口失败不阻塞 */ }
}`
  );
  // onMounted 里追加 loadTodos
  D = D.replace(/onMounted\((async)? ?\(\) => \{/, match => match + '\n  loadTodos()');
  // 样式
  D = D.replace('</style>', `
.todo-card { cursor: pointer; }
.todo-row { display: flex; justify-content: space-between; align-items: center; }
.todo-label { font-size: 14px; color: #606266; }
</style>`);
  fs.writeFileSync(dashPath, D, 'utf8');
}
console.log('dashboard todos:', D.includes('todoCards'));

// ================= views.ts 接线 =================

const viewsPath = path.join(UI, 'router/views.ts');
let v = fs.readFileSync(viewsPath, 'utf8');
const newImports = `import reportStockFlow from '@/views/report/stock-flow/index.vue'
import reportSupplierStatement from '@/views/report/supplier-statement/index.vue'
import systemMessage from '@/views/system/message/index.vue'
`;
if (!v.includes('views/system/message/index.vue')) {
  v = v.replace('export const viewModules', newImports + '\nexport const viewModules');
}
const newEntries = `  'report/stock-flow/index': reportStockFlow,
  'report/supplier-statement/index': reportSupplierStatement,
  'system/message/index': systemMessage,
`;
if (!v.includes("'system/message/index': systemMessage")) {
  v = v.replace("\n}\n", '\n' + newEntries + '}\n');
}
fs.writeFileSync(viewsPath, v, 'utf8');
console.log('views.ts wired:', v.includes("'system/message/index': systemMessage"));

console.log('gen-v16-ui done');
