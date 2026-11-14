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

// http.ts 增加 xlsx 下载助手
const httpPath = path.join(UI, 'api/http.ts');
let h = fs.readFileSync(httpPath, 'utf8');
if (!h.includes('downloadFile')) {
  h = h.replace('export default instance', `/** Blob 下载（带鉴权），用于报表/单据 xlsx 导出 */
export async function downloadFile(url: string, params: any, fileName: string) {
  const res: any = await instance.get(url, { params, responseType: 'blob' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(res.data)
  a.download = fileName
  a.click()
  URL.revokeObjectURL(a.href)
}

export default instance`);
  fs.writeFileSync(httpPath, h, 'utf8');
}
console.log('http downloadFile:', h.includes('downloadFile'));

// stock.ts 序列号 api
append('api/stock.ts', `
// ========== V17：序列号追溯 ==========
export const serialApi = {
  trace: (serialNo: string) => get<any[]>('/admin-api/stk/serial/trace', { serialNo }),
  inStock: (productId: string) => get<any[]>('/admin-api/stk/serial/in-stock', { productId }),
  register: (params: any) => post('/admin-api/stk/serial/register', null, params),
  consume: (params: any) => post('/admin-api/stk/serial/consume', null, params)
}
`, 'serialApi');

// sal/pur 订金 api
const salPath = path.join(UI, 'api/sal.ts');
let sal = fs.readFileSync(salPath, 'utf8');
if (!sal.includes('depositGet')) {
  sal = sal.replace('export const saleOrderApi = {', `export const saleOrderApi = {
  depositGet: (id: string) => get<any>('/admin-api/sal/order/deposit', { id }),
  depositRegister: (id: string, amount: number) =>
    post<number>('/admin-api/sal/order/deposit', null, { id, amount }),`);
  fs.writeFileSync(salPath, sal, 'utf8');
}
console.log('sal deposit api:', sal.includes('depositGet'));

const purPath = path.join(UI, 'api/pur.ts');
let pur = fs.readFileSync(purPath, 'utf8');
if (!pur.includes('depositGet')) {
  pur = pur.replace('export const purchaseOrderApi = {', `export const purchaseOrderApi = {
  depositGet: (id: string) => get<any>('/admin-api/pur/order/deposit', { id }),
  depositRegister: (id: string, amount: number) =>
    post<number>('/admin-api/pur/order/deposit', null, { id, amount }),`);
  fs.writeFileSync(purPath, pur, 'utf8');
}
console.log('pur deposit api:', pur.includes('depositGet'));

// mrp 成本 api
append('api/mrp.ts', `
// ========== V17：BOM 成本卷算 ==========
export const costApi = {
  rollup: (productId: string) => get<any>(\`\${PREFIX}/calc/cost\`, { productId })
}
`, 'costApi');

// ================= 序列号追溯页 =================

write('views/more/serial-trace/index.vue', `<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;gap:10px;align-items:center">
        <span>序列号追溯</span>
        <el-input v-model="serialNo" placeholder="输入序列号，如 SN20261114001" clearable style="width:280px"
                  @keyup.enter="trace" />
        <el-button type="primary" :disabled="!serialNo.trim()" @click="trace">追溯</el-button>
      </div>
    </template>

    <div v-if="rows.length">
      <el-descriptions :column="2" border style="margin-bottom:16px">
        <el-descriptions-item label="商品">{{ rows[0].productNo }} · {{ rows[0].productName }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="Number(rows[rows.length - 1].status) === 1 ? 'success' : 'info'">
            {{ Number(rows[rows.length - 1].status) === 1 ? '在库' : '已出库' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-steps direction="vertical" :active="rows.length">
        <el-step v-for="r in rows" :key="r.id"
                 :title="Number(r.status) === 1 ? '入库' : '出库'"
                 :description="(Number(r.status) === 1 ? r.inboundNo : r.outboundNo) + ' · ' +
                   fmtTime(Number(r.status) === 1 ? r.inboundTime : r.outboundTime)" />
      </el-steps>
    </div>
    <el-empty v-else-if="searched" description="未找到该序列号（入库/出库单明细备注中以逗号分隔写入序列号即可自动登记）" :image-size="80" />
    <el-empty v-else description="输入序列号开始追溯" :image-size="80" />

    <el-divider>按商品查询在库序列号</el-divider>
    <div style="display:flex;gap:10px;align-items:center;margin-bottom:12px">
      <el-select v-model="productId" filterable placeholder="选择商品" style="width:280px">
        <el-option v-for="p in products" :key="p.value" :label="p.label" :value="p.value" />
      </el-select>
      <el-button :disabled="!productId" @click="loadStock">查询在库</el-button>
      <el-button :disabled="!productId" @click="manualRegister">手工登记</el-button>
    </div>
    <el-table :data="stockRows" border size="small">
      <el-table-column prop="serialNo" label="序列号" min-width="180" />
      <el-table-column label="入库单号" prop="inboundNo" width="180" />
      <el-table-column label="入库时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.inboundTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" class-name="op-cell">
        <template #default="{ row }">
          <div class="op-group">
            <el-button link type="warning" @click="manualConsume(row)">核销</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { serialApi } from '@/api/stock'
import { productApi } from '@/api/bas'

const serialNo = ref('')
const rows = ref<any[]>([])
const searched = ref(false)
const products = ref<any[]>([])
const productId = ref('')
const stockRows = ref<any[]>([])

onMounted(async () => {
  const page = await productApi.page({ pageNo: 1, pageSize: 300 })
  products.value = (page.list || []).map((p: any) => ({ label: p.no + ' ' + p.name, value: p.id }))
})

async function trace() {
  rows.value = (await serialApi.trace(serialNo.value.trim())) || []
  searched.value = true
}

async function loadStock() {
  stockRows.value = (await serialApi.inStock(productId.value)) || []
}

async function manualRegister() {
  const { value } = await ElMessageBox.prompt('输入序列号（多个用逗号分隔）', '手工登记', { type: 'info' })
  const sns = value.split(/[,，\s]+/).filter(Boolean)
  for (const sn of sns) {
    await serialApi.register({ serialNo: sn, productId: productId.value, bizNo: 'MANUAL' })
  }
  ElMessage.success('已登记 ' + sns.length + ' 个序列号')
  await loadStock()
}

async function manualConsume(row: any) {
  await ElMessageBox.confirm('确认核销序列号 ' + row.serialNo + '？', '提示', { type: 'warning' })
  await serialApi.consume({ serialNo: row.serialNo, productId: productId.value, bizNo: 'MANUAL' })
  ElMessage.success('已核销')
  await loadStock()
}

function fmtTime(t: string) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}
</script>
`)

// ================= OrderPage 订金支持 =================

const orderPagePath = path.join(UI, 'components/OrderPage.vue');
let O = fs.readFileSync(orderPagePath, 'utf8');
if (!O.includes('depositDialog')) {
  // 1) 接口定义：OrderConfig 增加可选 deposit
  O = O.replace(`  /** detail 响应中表头字段名：order / inbound / outbound */
  headKey: string`,
    `  /** detail 响应中表头字段名：order / inbound / outbound */
  headKey: string
  /** 订金管理（销售/采购订单可选配置） */
  deposit?: {
    get: (id: string) => Promise<any>
    register: (id: string, amount: number) => Promise<any>
  }`);
  // 2) 下拉菜单加 登记订金
  O = O.replace('                  <el-dropdown-item command="attach">附件</el-dropdown-item>',
    `                  <el-dropdown-item command="attach">附件</el-dropdown-item>
                  <el-dropdown-item v-if="config.deposit" command="deposit">订金</el-dropdown-item>`);
  // 3) 命令分发
  O = O.replace("    case 'attach': openAttach(row); break",
    `    case 'attach': openAttach(row); break
    case 'deposit': openDeposit(row); break`);
  // 4) 订金状态与方法（插在附件逻辑后）
  O = O.replace(`function fmtAttachSize(n: number) {`,
    `// ========== 订金（销售/采购订单） ==========
const depositDialog = ref(false)
const depositRow = ref<any>(null)
const depositCurrent = ref(0)
const depositTotal = ref(0)
const depositAmount = ref<number>(1)
const depositSaving = ref(false)

async function openDeposit(row: any) {
  if (!props.config.deposit) return
  const order = await props.config.deposit.get(row.id)
  depositRow.value = row
  depositCurrent.value = Number(order.depositAmount || 0)
  depositTotal.value = Number(order.totalAmountTax || order.totalAmount || 0)
  depositAmount.value = 1
  depositDialog.value = true
}

async function saveDeposit() {
  if (!props.config.deposit || !depositRow.value) return
  depositSaving.value = true
  try {
    await props.config.deposit.register(depositRow.value.id, depositAmount.value)
    ElMessage.success('订金已登记')
    depositDialog.value = false
    loadList()
  } finally {
    depositSaving.value = false
  }
}

function fmtAttachSize(n: number) {`);
  // 5) 对话框模板（插在附件对话框后）
  O = O.replace(`      </el-dialog>
    </el-card>
  </div>
</template>`,
    `      </el-dialog>

      <!-- 订金登记对话框 -->
      <el-dialog v-model="depositDialog" :title="'订金 · ' + config.title" width="480px">
        <el-descriptions :column="2" border style="margin-bottom:16px">
          <el-descriptions-item label="单号">{{ depositRow?.no }}</el-descriptions-item>
          <el-descriptions-item label="订单总额">{{ depositTotal }}</el-descriptions-item>
          <el-descriptions-item label="已收订金">{{ depositCurrent }}</el-descriptions-item>
          <el-descriptions-item label="剩余待收">{{ Math.max(depositTotal - depositCurrent, 0) }}</el-descriptions-item>
        </el-descriptions>
        <el-input-number v-model="depositAmount" :min="0.01" :precision="2" :controls="false"
                         style="width: 100%" placeholder="本次登记金额" />
        <template #footer>
          <el-button @click="depositDialog = false">取消</el-button>
          <el-button type="primary" :loading="depositSaving" @click="saveDeposit">登记订金</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>`);
  fs.writeFileSync(orderPagePath, O, 'utf8');
}
console.log('OrderPage deposit:', O.includes('depositDialog'));

// 订单页 config 挂 deposit
for (const [page, apiName] of [['views/sal/order/index.vue', 'saleOrderApi'], ['views/pur/order/index.vue', 'purchaseOrderApi']]) {
  const fp = path.join(UI, page);
  let c = fs.readFileSync(fp, 'utf8');
  if (!c.includes('deposit:')) {
    c = c.replace(`  headKey: 'order',`, `  headKey: 'order',
  deposit: { get: ${apiName}.depositGet, register: ${apiName}.depositRegister },`);
    fs.writeFileSync(fp, c, 'utf8');
  }
  console.log(page, 'deposit wired:', c.includes('deposit:'));
}

// ================= mrp/calc 成本卷算面板 =================

const calcPath = path.join(UI, 'views/mrp/calc/index.vue');
let C = fs.readFileSync(calcPath, 'utf8');
if (!C.includes('costRollup')) {
  C = C.replace('</template>', `
    <!-- V17：BOM 成本卷算 -->
    <el-card shadow="never" style="margin-top: 16px">
      <div class="table-toolbar">
        <span style="font-weight: 600">BOM 成本卷算</span>
        <span style="color: #909399; font-size: 12px">按默认 BOM 递归卷算材料成本（叶子取采购价），对比销售价得毛利率</span>
      </div>
      <div style="display:flex;gap:10px;align-items:center;margin-bottom:12px">
        <el-select v-model="costProductId" filterable placeholder="选择产品" style="width:280px">
          <el-option v-for="p in products" :key="p.value" :label="p.label" :value="p.value" />
        </el-select>
        <el-button type="primary" :disabled="!costProductId" :loading="costLoading" @click="runCost">卷算成本</el-button>
      </div>
      <div v-if="costResult">
        <el-tag size="large" style="margin-bottom:12px">
          {{ costResult.productName }} 单位材料成本：{{ Number(costResult.totalCost).toFixed(2) }}
        </el-tag>
        <el-table :data="costLines" border size="small">
          <el-table-column prop="productName" label="产品" min-width="160" />
          <el-table-column prop="source" label="来源" width="170" />
          <el-table-column prop="qty" label="用量(份)" width="110" align="right" />
          <el-table-column prop="unitPrice" label="单价" width="110" align="right" />
          <el-table-column label="成本" width="110" align="right">
            <template #default="{ row }">{{ Number(row.cost).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
</template>`);
  // script：状态与方法（锚定 script setup 末尾或既有 onMounted）
  C = C.replace("<script setup lang=\"ts\">", `<script setup lang="ts">
import { costApi } from '@/api/mrp'`);
  // 在最后一个 </script> 前插入逻辑（简单起见追加独立块）
  C = C.replace('</script>', `
const costProductId = ref('')
const costLoading = ref(false)
const costResult = ref<any>(null)
const costLines = computed(() => (costResult.value?.lines || []).filter((l: any) => l.unitPrice != null))

async function runCost() {
  costLoading.value = true
  try {
    costResult.value = await costApi.rollup(costProductId.value)
  } finally {
    costLoading.value = false
  }
}
</script>`);
  fs.writeFileSync(calcPath, C, 'utf8');
}
console.log('calc cost panel:', C.includes('costRollup') || C.includes('runCost'));

// ================= 导入页收 xlsx =================

const importPagePath = path.join(UI, 'views/infra/import/index.vue');
let I = fs.readFileSync(importPagePath, 'utf8');
if (!I.includes('.xlsx')) {
  I = I.replace('accept=".csv,.txt"', 'accept=".csv,.txt,.xlsx,.xls"');
  I = I.replace('CSV，UTF-8 编码，英文逗号分隔，首行表头自动跳过', 'CSV / Excel(xlsx)，首行表头自动跳过');
  fs.writeFileSync(importPagePath, I, 'utf8');
}
console.log('import xlsx accept:', I.includes('.xlsx'));

// ================= 大屏页 =================

write('views/screen/index.vue', `<template>
  <div class="screen" :class="{ dark: darkMode }">
    <div class="screen-header">
      <span class="screen-title">天工 ERP · 经营大屏</span>
      <div class="screen-actions no-print">
        <el-button size="small" @click="toggleDark">{{ darkMode ? '浅色' : '深色' }}</el-button>
        <el-button size="small" @click="exit">退出大屏</el-button>
      </div>
    </div>

    <el-row :gutter="20" class="screen-cards">
      <el-col :span="4" v-for="card in cards" :key="card.label">
        <div class="screen-card">
          <div class="screen-card-label">{{ card.label }}</div>
          <div class="screen-card-value">{{ card.value }}</div>
          <div class="screen-card-suffix">{{ card.suffix }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="screen-charts">
      <el-col :span="16">
        <div class="screen-panel">
          <div class="screen-panel-title">销售 / 采购月度趋势</div>
          <div ref="trendRef" class="screen-trend" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="screen-panel">
          <div class="screen-panel-title">待办与预警</div>
          <div class="screen-todo">
            <div v-for="t in todos" :key="t.label" class="screen-todo-item">
              <span>{{ t.label }}</span>
              <span class="screen-todo-value" :class="{ hot: t.value > 0 }">{{ t.value }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api/rpt'
import { bpmApi } from '@/api/bpm'
import { messageApi } from '@/api/system'
import { alertScanApi } from '@/api/stock'

const darkMode = ref(true)
const summary = ref<any>(null)
const trendRef = ref<HTMLDivElement>()
const todoCount = ref(0)
const unreadCount = ref(0)
const lowCount = ref(0)
let chart: echarts.ECharts | null = null
let timer: any = null

const cards = computed(() => [
  { label: '本月销售额', value: fmt(latestAmount(summary.value?.salesTrend)), suffix: '元' },
  { label: '本月采购额', value: fmt(latestAmount(summary.value?.purchaseTrend)), suffix: '元' },
  { label: '库存总金额', value: fmt(summary.value?.stockValue), suffix: '元' },
  { label: '在产工单', value: summary.value?.activeWorkOrders ?? 0, suffix: '张' },
  { label: '未核销应收', value: fmt(summary.value?.unsettledReceivable), suffix: '元' },
  { label: '未核销应付', value: fmt(summary.value?.unsettledPayable), suffix: '元' }
])

const todos = computed(() => [
  { label: '待办审批任务', value: todoCount.value },
  { label: '未读站内消息', value: unreadCount.value },
  { label: '低库存预警商品', value: lowCount.value }
])

function fmt(v: any) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function latestAmount(trend: any) {
  if (!Array.isArray(trend) || !trend.length) return 0
  return trend[trend.length - 1]?.amount ?? 0
}

async function load() {
  summary.value = await dashboardApi.summary()
  try {
    const tasks = await bpmApi.myTasks()
    todoCount.value = Array.isArray(tasks) ? tasks.length : 0
  } catch { }
  try {
    unreadCount.value = Number(await messageApi.myUnreadCount()) || 0
  } catch { }
  try {
    lowCount.value = ((await alertScanApi.lowList()) || []).length
  } catch { }
  renderTrend()
}

function renderTrend() {
  if (!trendRef.value) return
  if (!chart) chart = echarts.init(trendRef.value)
  const sales = summary.value?.salesTrend || []
  const pur = summary.value?.purchaseTrend || []
  const textColor = darkMode.value ? '#c8c9cc' : '#333'
  chart.setOption({
    textStyle: { color: textColor },
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '采购额'], textStyle: { color: textColor } },
    grid: { left: 60, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: sales.map((s: any) => s.month), axisLabel: { color: textColor } },
    yAxis: { type: 'value', axisLabel: { color: textColor }, splitLine: { lineStyle: { opacity: 0.2 } } },
    series: [
      { name: '销售额', type: 'bar', data: sales.map((s: any) => s.amount), itemStyle: { color: '#409eff' } },
      { name: '采购额', type: 'bar', data: pur.map((s: any) => s.amount), itemStyle: { color: '#67c23a' } }
    ]
  })
}

function toggleDark() {
  darkMode.value = !darkMode.value
  setTimeout(renderTrend, 100)
}

function enterFullscreen() {
  const el = document.documentElement
  if (el.requestFullscreen) el.requestFullscreen().catch(() => { })
}

function exit() {
  if (document.fullscreenElement) document.exitFullscreen()
  history.back()
}

onMounted(async () => {
  await load()
  enterFullscreen()
  timer = setInterval(load, 30000)
  window.addEventListener('resize', renderTrend)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', renderTrend)
})
</script>

<style scoped>
.screen { padding: 20px; min-height: 100vh; background: #f0f2f5; color: #333; transition: all .3s; }
.screen.dark { background: #0d1117; color: #c8c9cc; }
.screen-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.screen-title { font-size: 26px; font-weight: 700; letter-spacing: 4px; }
.screen-card { border: 1px solid rgba(144, 147, 153, .25); border-radius: 8px; padding: 16px; text-align: center; }
.dark .screen-card { background: rgba(255, 255, 255, .04); }
.screen-card-label { font-size: 14px; opacity: .75; }
.screen-card-value { font-size: 30px; font-weight: 700; margin: 6px 0; font-variant-numeric: tabular-nums; }
.screen-card-suffix { font-size: 12px; opacity: .6; }
.screen-panel { border: 1px solid rgba(144, 147, 153, .25); border-radius: 8px; padding: 16px; height: 100%; }
.dark .screen-panel { background: rgba(255, 255, 255, .04); }
.screen-panel-title { font-size: 16px; font-weight: 600; margin-bottom: 10px; }
.screen-trend { height: 320px; }
.screen-todo-item { display: flex; justify-content: space-between; align-items: center; padding: 18px 8px; border-bottom: 1px dashed rgba(144, 147, 153, .3); font-size: 16px; }
.screen-todo-value { font-size: 30px; font-weight: 700; font-variant-numeric: tabular-nums; }
.screen-todo-value.hot { color: #e6a23c; }
.dark .screen-todo-value.hot { color: #f56c6c; }
</style>
`)

// ================= 静态路由 /screen =================

const routerPath = path.join(UI, 'router/index.ts');
let r = fs.readFileSync(routerPath, 'utf8');
if (!r.includes("'/screen'")) {
  const anchor = "path: '/print/order',";
  const i = r.indexOf(anchor);
  if (i < 0) throw new Error('print route anchor not found');
  const blockStart = r.indexOf('{', i - 6);
  const insertAt = r.indexOf('},', i) + 2;
  const screenRoute = `
    {
      path: '/screen',
      name: 'Screen',
      component: () => import('@/views/screen/index.vue'),
      meta: { title: '经营大屏' }
    },`;
  r = r.slice(0, insertAt) + screenRoute + r.slice(insertAt);
  fs.writeFileSync(routerPath, r, 'utf8');
}
console.log('router /screen:', r.includes("'/screen'"));

// ================= 工作台入口按钮 =================

const dashPath = path.join(UI, 'views/dashboard/index.vue');
let D = fs.readFileSync(dashPath, 'utf8');
if (!D.includes('/screen')) {
  D = D.replace(`    <!-- 待办提醒 -->`,
    `    <div class="table-toolbar" style="margin-top:16px">
      <span></span>
      <el-button type="warning" @click="router.push('/screen')">进入经营大屏</el-button>
    </div>

    <!-- 待办提醒 -->`);
  fs.writeFileSync(dashPath, D, 'utf8');
}
console.log('dashboard screen button:', D.includes('/screen'));

// ================= views.ts 接线 =================

const viewsPath = path.join(UI, 'router/views.ts');
let v = fs.readFileSync(viewsPath, 'utf8');
const newImports = `import moreSerialTrace from '@/views/more/serial-trace/index.vue'
`;
if (!v.includes('views/more/serial-trace/index.vue')) {
  v = v.replace('export const viewModules', newImports + '\nexport const viewModules');
}
const newEntries = `  'more/serial-trace/index': moreSerialTrace,
`;
if (!v.includes("'more/serial-trace/index': moreSerialTrace")) {
  v = v.replace("\n}\n", '\n' + newEntries + '}\n');
}
fs.writeFileSync(viewsPath, v, 'utf8');
console.log('views.ts wired:', v.includes("'more/serial-trace/index': moreSerialTrace"));

console.log('gen-v17-ui done');
