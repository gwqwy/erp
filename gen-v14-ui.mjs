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

// ================= api 追加 =================

append('api/rpt.ts', `
// ========== 经营分析（V14）：排行 / 库存 ABC / 客户对账单 ==========
export const analysisApi = {
  saleRank: (params?: any) => get<any[]>('/admin-api/rpt/sale-rank', params),
  purchaseRank: (params?: any) => get<any[]>('/admin-api/rpt/purchase-rank', params),
  stockAbc: () => get<any[]>('/admin-api/rpt/stock-abc'),
  customerStatement: (params: { partnerId: string; from: string; to: string }) =>
    get<any>('/admin-api/rpt/customer-statement', params)
}
`, 'analysisApi');

append('api/infra.ts', `
// ========== CSV 数据导入（V14） ==========
export const bulkImportApi = {
  upload: (kind: 'products' | 'partners' | 'stock-openings', file: File) => {
    const form = new FormData()
    form.append('file', file)
    return instance.post('/admin-api/infra/import/' + kind, form)
  }
}
`, 'bulkImportApi');

append('api/stock.ts', `
// ========== 库存预警扫描（V14） ==========
export const alertScanApi = {
  scanNow: () => post<any>('/admin-api/stk/alert/scan'),
  lowList: () => get<any[]>('/admin-api/stk/alert/low-list')
}
`, 'alertScanApi');

// 检查 stock.ts / infra.ts 的 http 导入是否含 post/instance
const stockApiSrc = fs.readFileSync(path.join(UI, 'api/stock.ts'), 'utf8');
console.log('stock.ts imports:', stockApiSrc.split('\n')[0]);
const infraSrc = fs.readFileSync(path.join(UI, 'api/infra.ts'), 'utf8');
console.log('infra.ts imports:', infraSrc.split('\n')[0]);
const rptSrc = fs.readFileSync(path.join(UI, 'api/rpt.ts'), 'utf8');
console.log('rpt.ts imports:', rptSrc.split('\n')[0]);

// ================= 数据导入页 =================

write('views/infra/import/index.vue', `<template>
  <el-card shadow="never">
    <template #header>
      <span>数据导入（CSV，UTF-8 编码，英文逗号分隔，首行表头自动跳过）</span>
    </template>
    <el-tabs v-model="tab">
      <el-tab-pane label="商品导入" name="products">
        <div class="imp-tip">
          列格式：<b>编码,名称,规格,采购价,销售价,安全库存</b><br />
          按编码匹配：已存在则更新名称/规格/价格/安全库存，不存在则新增。
        </div>
      </el-tab-pane>
      <el-tab-pane label="往来单位导入" name="partners">
        <div class="imp-tip">
          列格式：<b>名称,类型(customer|supplier|both),联系人,电话,地址,备注</b><br />
          按名称匹配：已存在则更新联系人/电话/地址，不存在则新增。
        </div>
      </el-tab-pane>
      <el-tab-pane label="库存期初导入" name="stock-openings">
        <div class="imp-tip">
          列格式：<b>仓库编码,商品编码,数量,成本价</b><br />
          按仓库+商品定位库存：已存在则累加数量，不存在则新建期初批次（INIT）。
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-upload :show-file-list="false" :auto-upload="false" accept=".csv,.txt" :on-change="onPick">
      <el-button type="primary" :loading="uploading">选择 CSV 并导入</el-button>
    </el-upload>

    <el-alert v-if="result" :type="result.fail === 0 ? 'success' : 'warning'" :closable="false" style="margin-top:12px">
      <div>共 {{ result.total }} 行：成功 {{ result.success }}，失败 {{ result.fail }}</div>
      <div v-for="e in result.errors" :key="e" style="color:#e6a23c">{{ e }}</div>
    </el-alert>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { bulkImportApi } from '@/api/infra'

const tab = ref('products')
const uploading = ref(false)
const result = ref<any>(null)

async function onPick(file: any) {
  const raw: File = file.raw
  if (!raw) return
  uploading.value = true
  result.value = null
  try {
    const res: any = await bulkImportApi.upload(tab.value as any, raw)
    result.value = res
    if (res.fail === 0) ElMessage.success('导入成功：' + res.success + ' 条')
    else ElMessage.warning('成功 ' + res.success + ' 条，失败 ' + res.fail + ' 条')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.imp-tip { color: #909399; font-size: 13px; line-height: 1.9; margin-bottom: 12px; }
</style>
`)

// ================= 4 个报表页 =================

write('views/report/rank-sale/index.vue', `<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>销售商品排行（已审核订单，按金额）</span>
        <div style="display:flex;gap:8px;align-items:center">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始" end-placeholder="结束" style="width:260px" />
          <el-button type="primary" @click="load">查询</el-button>
        </div>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="productName" label="商品" min-width="200" />
      <el-table-column prop="qty" label="销售数量" width="140" align="right" />
      <el-table-column prop="amount" label="销售金额" width="160" align="right" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { analysisApi } from '@/api/rpt'

const rows = ref<any[]>([])
const loading = ref(false)
const range = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const params: any = { limit: 20 }
    if (range.value) { params.from = range.value[0]; params.to = range.value[1] }
    rows.value = await analysisApi.saleRank(params)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
`)

write('views/report/rank-purchase/index.vue', `<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>采购商品排行（已审核订单，按金额）</span>
        <div style="display:flex;gap:8px;align-items:center">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始" end-placeholder="结束" style="width:260px" />
          <el-button type="primary" @click="load">查询</el-button>
        </div>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="productName" label="商品" min-width="200" />
      <el-table-column prop="qty" label="采购数量" width="140" align="right" />
      <el-table-column prop="amount" label="采购金额" width="160" align="right" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { analysisApi } from '@/api/rpt'

const rows = ref<any[]>([])
const loading = ref(false)
const range = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const params: any = { limit: 20 }
    if (range.value) { params.from = range.value[0]; params.to = range.value[1] }
    rows.value = await analysisApi.purchaseRank(params)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
`)

write('views/report/stock-abc/index.vue', `<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>库存 ABC 分析（按库存占用金额：累计 80% 为 A，80~95% 为 B，其余 C）</span>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" border stripe :row-class-name="rowClass">
      <el-table-column prop="productName" label="商品" min-width="200" />
      <el-table-column prop="qty" label="库存数量" width="130" align="right" />
      <el-table-column prop="amount" label="占用金额" width="150" align="right" />
      <el-table-column label="金额占比" width="110" align="right">
        <template #default="{ row }">{{ (Number(row.amountRatio) * 100).toFixed(2) }}%</template>
      </el-table-column>
      <el-table-column label="累计占比" width="110" align="right">
        <template #default="{ row }">{{ (Number(row.cumulativeRatio) * 100).toFixed(2) }}%</template>
      </el-table-column>
      <el-table-column label="分类" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.abcClass === 'A' ? 'danger' : row.abcClass === 'B' ? 'warning' : 'info'">
            {{ row.abcClass }} 类
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { analysisApi } from '@/api/rpt'

const rows = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await analysisApi.stockAbc()
  } finally {
    loading.value = false
  }
}

function rowClass({ row }: any) {
  return row.abcClass === 'A' ? 'abc-a' : ''
}

onMounted(load)
</script>

<style scoped>
:deep(.abc-a) { background: #fef0f0; }
</style>
`)

write('views/report/customer-statement/index.vue', `<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
        <span>客户对账单</span>
        <el-select v-model="partnerId" filterable placeholder="选择客户" style="width:220px">
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
        <el-tag type="warning">本期发货：{{ data.totalDebit }}</el-tag>
        <el-tag type="success">本期回款：{{ data.totalCredit }}</el-tag>
        <el-tag type="danger">期末应收：{{ data.closing }}</el-tag>
      </div>
      <el-table :data="data.rows" v-loading="loading" border stripe show-summary
                :summary-method="summary">
        <el-table-column prop="date" label="日期" width="130" />
        <el-table-column prop="kind" label="业务类型" width="110" />
        <el-table-column prop="no" label="单号" width="180" />
        <el-table-column label="发货(借)" width="140" align="right">
          <template #default="{ row }">{{ Number(row.debit) !== 0 ? row.debit : '' }}</template>
        </el-table-column>
        <el-table-column label="回款(贷)" width="140" align="right">
          <template #default="{ row }">{{ Number(row.credit) !== 0 ? row.credit : '' }}</template>
        </el-table-column>
        <el-table-column prop="balance" label="余额" align="right" />
      </el-table>
      <el-empty v-if="data && (!data.rows || !data.rows.length) && !loading" description="该期间无往来明细" :image-size="70" />
    </div>
    <el-empty v-else description="选择客户与期间后查询" :image-size="80" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { analysisApi } from '@/api/rpt'
import { partnerApi } from '@/api/bas'

const partners = ref<any[]>([])
const partnerId = ref('')
const range = ref<any>(null)
const data = ref<any>(null)
const loading = ref(false)

onMounted(async () => {
  const page = await partnerApi.page({ pageNo: 1, pageSize: 200 })
  partners.value = (page.list || []).filter((p: any) => p.isCustomer)
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
    data.value = await analysisApi.customerStatement(params)
  } finally {
    loading.value = false
  }
}

function summary({ columns, data }: any) {
  return columns.map((col: any, i: number) => {
    if (i === 0) return '合计'
    if (col.property === 'debit') return data.reduce((s: number, r: any) => s + Number(r.debit || 0), 0).toFixed(2)
    if (col.property === 'credit') return data.reduce((s: number, r: any) => s + Number(r.credit || 0), 0).toFixed(2)
    if (col.property === 'balance') return data.value?.closing ?? ''
    return ''
  })
}

function print() {
  window.print()
}
</script>
`)

// ================= 打印页 =================

write('views/print/order/index.vue', `<template>
  <div class="print-page">
    <div class="print-toolbar no-print">
      <el-button type="primary" @click="doPrint">打 印</el-button>
      <el-button @click="close">关 闭</el-button>
    </div>

    <div v-if="head" class="print-doc">
      <h2 class="print-title">{{ title }}</h2>
      <div class="print-meta">
        <span>单号：<b>{{ head.no }}</b></span>
        <span>日期：{{ head.orderDate || head.outboundDate || head.inboundDate || '' }}</span>
        <span>状态：{{ statusText }}</span>
      </div>
      <div class="print-meta">
        <span>{{ partnerLabel }}：<b>{{ partnerName }}</b></span>
      </div>
      <table class="print-table">
        <thead>
          <tr>
            <th style="width:50px">#</th>
            <th>商品</th>
            <th style="width:110px">数量</th>
            <th style="width:120px">单价</th>
            <th style="width:130px">金额</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(it, i) in items" :key="i">
            <td>{{ i + 1 }}</td>
            <td>{{ productName(it.productId) }}</td>
            <td>{{ it.qty }}</td>
            <td>{{ it.price }}</td>
            <td>{{ it.amount ?? (Number(it.qty || 0) * Number(it.price || 0)).toFixed(2) }}</td>
            <td>{{ it.remark || '' }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td colspan="2">合计</td>
            <td>{{ totalQty }}</td>
            <td></td>
            <td>{{ totalAmount }}</td>
            <td></td>
          </tr>
        </tfoot>
      </table>
      <div class="print-remark" v-if="head.remark">备注：{{ head.remark }}</div>
      <div class="print-sign">
        <span>制单人：__________</span>
        <span>审核人：__________</span>
        <span>客户确认：__________</span>
      </div>
    </div>
    <el-empty v-else description="单据不存在" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { saleOrderApi, saleOutboundApi } from '@/api/sal'
import { purchaseOrderApi, purchaseInboundApi } from '@/api/pur'
import { partnerApi, productApi } from '@/api/bas'

const route = useRoute()
const head = ref<any>(null)
const items = ref<any[]>([])
const partners = ref<any[]>([])
const products = ref<any[]>([])

const TYPE_MAP: Record<string, { api: () => any; headKey: string; title: string; partnerProp: string; partnerType: 'customer' | 'supplier' }> = {
  sal_order: { api: () => saleOrderApi, headKey: 'order', title: '销售订单', partnerProp: 'customerId', partnerType: 'customer' },
  pur_order: { api: () => purchaseOrderApi, headKey: 'order', title: '采购订单', partnerProp: 'supplierId', partnerType: 'supplier' },
  sal_outbound: { api: () => saleOutboundApi, headKey: 'outbound', title: '销售出库单', partnerProp: 'customerId', partnerType: 'customer' },
  pur_inbound: { api: () => purchaseInboundApi, headKey: 'inbound', title: '采购入库单', partnerProp: 'supplierId', partnerType: 'supplier' }
}

const conf = computed(() => TYPE_MAP[String(route.query.type || '')])
const title = computed(() => conf.value?.title || '单据')
const partnerLabel = computed(() => conf.value?.partnerType === 'supplier' ? '供应商' : '客户')
const partnerName = computed(() => {
  const id = head.value?.[conf.value?.partnerProp || '']
  return partners.value.find(p => String(p.id) === String(id))?.name ?? id ?? ''
})
const statusText = computed(() => ({ 10: '草稿', 20: '审批中', 30: '已审核', 40: '已完成', 50: '已作废', 60: '已冲销' }[Number(head.value?.status)] || head.value?.status))
const totalQty = computed(() => items.value.reduce((s, i) => s + Number(i.qty || 0), 0))
const totalAmount = computed(() => items.value.reduce((s, i) => s + Number(i.amount ?? Number(i.qty || 0) * Number(i.price || 0)), 0).toFixed(2))

function productName(id: any) {
  return products.value.find(p => String(p.id) === String(id))?.name ?? id
}

function close() { window.close() }

function doPrint() { window.print() }

onMounted(async () => {
  const type = String(route.query.type || '')
  const id = String(route.query.id || '')
  const conf = TYPE_MAP[type]
  if (!conf || !id) return
  const [detail, partnerPage, productPage] = await Promise.all([
    conf.api().detail(id),
    partnerApi.page({ pageNo: 1, pageSize: 200 }),
    productApi.page({ pageNo: 1, pageSize: 500 })
  ])
  head.value = detail[conf.headKey]
  items.value = detail.items || []
  partners.value = partnerPage.list || []
  products.value = productPage.list || []
})
</script>

<style scoped>
.print-page { padding: 24px; background: #f0f2f5; min-height: 100vh; }
.print-toolbar { margin-bottom: 16px; }
.print-doc { background: #fff; margin: 0 auto; padding: 40px 48px; max-width: 860px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.print-title { text-align: center; margin: 0 0 18px; letter-spacing: 6px; }
.print-meta { display: flex; gap: 32px; margin-bottom: 8px; font-size: 13px; color: #333; }
.print-table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 13px; }
.print-table th, .print-table td { border: 1px solid #333; padding: 7px 9px; text-align: left; }
.print-table th { background: #f5f5f5; text-align: center; }
.print-table tfoot td { font-weight: 700; background: #fafafa; }
.print-remark { margin-top: 12px; font-size: 13px; color: #333; }
.print-sign { display: flex; justify-content: space-between; margin-top: 56px; font-size: 13px; }

@media print {
  .print-page { background: #fff; padding: 0; }
  .no-print { display: none !important; }
  .print-doc { box-shadow: none; max-width: none; padding: 0; }
}
</style>
`)

// ================= router 静态路由 /print/order =================

const routerPath = path.join(UI, 'router/index.ts');
let r = fs.readFileSync(routerPath, 'utf8');
if (!r.includes('/print/order')) {
  // 锚定 login 路由块（不依赖 title 文本，避免编码差异）
  const loginAnchor = "component: () => import('@/views/login/index.vue'),";
  const i = r.indexOf(loginAnchor);
  if (i < 0) throw new Error('login route anchor not found');
  const insertAt = r.indexOf('},', i) + 2;
  const printRoute = `
    {
      path: '/print/order',
      name: 'PrintOrder',
      component: () => import('@/views/print/order/index.vue'),
      meta: { title: '单据打印' }
    },`;
  r = r.slice(0, insertAt) + printRoute + r.slice(insertAt);
  fs.writeFileSync(routerPath, r, 'utf8');
}
console.log('router /print/order:', r.includes('/print/order'));

// ================= stk/alert 页加扫描按钮（重写） =================

write('views/stk/alert/index.vue', `<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">库存预警（低于安全下限 / 高于上限即超储）</span>
        <div style="display:flex;gap:8px">
          <el-button type="warning" :loading="scanning" @click="scanNow">立即扫描并通知</el-button>
          <el-button :icon="Refresh" circle @click="load" />
        </div>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="productName" label="产品" min-width="160" />
        <el-table-column prop="totalQty" label="当前总库存" width="120" align="right" />
        <el-table-column prop="stockMin" label="安全下限" width="110" align="right" />
        <el-table-column prop="stockMax" label="上限" width="110" align="right" />
        <el-table-column label="预警级别" width="140">
          <template #default="{ row }">
            <el-tag :type="row.level === 'low' ? 'danger' : 'warning'">
              {{ row.level === 'low' ? '⚠ 低于安全下限' : '⚠ 超储' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!rows.length && !loading" description="全部正常，无预警 🎉" :image-size="80" />
      <div class="alert-tip">每天 08:00 系统自动扫描，命中低库存会向管理员发送站内消息；也可点击右上角手动扫描。</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { alertScanApi } from '@/api/stock'

const loading = ref(false)
const scanning = ref(false)
const rows = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    const res = await fetch('/admin-api/stk/alert/list', {
      headers: { Authorization: 'Bearer ' + (localStorage.getItem('tiangong_token') || '') }
    }).then(r => r.json())
    rows.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

async function scanNow() {
  scanning.value = true
  try {
    const res: any = await alertScanApi.scanNow()
    if (res.count === 0) ElMessage.success('扫描完成：无低库存商品')
    else if (res.notified) ElMessage.warning('扫描完成：' + res.count + ' 个商品低库存，已发送站内消息')
    else ElMessage.info('扫描完成：' + res.count + ' 个商品低库存')
    await load()
  } finally {
    scanning.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.alert-tip { color: #c0c4cc; font-size: 12px; margin-top: 10px; }
</style>
`)

// ================= views.ts 接线 =================

const viewsPath = path.join(UI, 'router/views.ts');
let v = fs.readFileSync(viewsPath, 'utf8');
const newImports = `import reportRankSale from '@/views/report/rank-sale/index.vue'
import reportRankPurchase from '@/views/report/rank-purchase/index.vue'
import reportStockAbc from '@/views/report/stock-abc/index.vue'
import reportCustomerStatement from '@/views/report/customer-statement/index.vue'
import infraImport from '@/views/infra/import/index.vue'
`;
if (!v.includes('views/infra/import/index.vue')) {
  v = v.replace('export const viewModules', newImports + '\nexport const viewModules');
}
const newEntries = `  'report/rank-sale/index': reportRankSale,
  'report/rank-purchase/index': reportRankPurchase,
  'report/stock-abc/index': reportStockAbc,
  'report/customer-statement/index': reportCustomerStatement,
  'infra/import/index': infraImport,
`;
if (!v.includes("'infra/import/index': infraImport")) {
  v = v.replace("\n}\n", '\n' + newEntries + '}\n');
}
fs.writeFileSync(viewsPath, v, 'utf8');
console.log('views.ts wired:', v.includes("'infra/import/index': infraImport"));

console.log('gen-v14-ui done');
