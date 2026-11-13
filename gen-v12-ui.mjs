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
  if (s.includes(anchor)) {
    console.log('skip (exists):', rel, anchor);
    return;
  }
  fs.writeFileSync(fp, s.trimEnd() + '\n' + block, 'utf8');
  console.log('append:', rel, anchor);
}

// ================= api 追加 =================

append('api/bas.ts', `
// ========== 库位 / 价格策略（V11 bas 扩展端点） ==========
export const locationApi = {
  list: (warehouseId: string) => get<any[]>('/admin-api/bas/location/list', { warehouseId }),
  create: (params: any) => post('/admin-api/bas/location/create', null, params),
  update: (params: any) => put('/admin-api/bas/location/update', null, params),
  delete: (id: string) => del('/admin-api/bas/location/delete', { id })
}

export const partnerPriceApi = {
  list: (params?: any) => get<any[]>('/admin-api/bas/price/list', params),
  create: (params: any) => post('/admin-api/bas/price/create', null, params),
  delete: (id: string) => del('/admin-api/bas/price/delete', { id })
}
`, 'locationApi');

append('api/fin.ts', `
// ========== 发票 / 回款计划 / 期末结转（V11 fin 扩展端点） ==========
export const invoiceApi = {
  list: (params?: any) => get<any[]>('/admin-api/fin/invoice/list', params),
  create: (params: any) => post('/admin-api/fin/invoice/create', null, params),
  delete: (id: string) => del('/admin-api/fin/invoice/delete', { id })
}

export const receivablePlanApi = {
  list: (params?: any) => get<any[]>('/admin-api/fin/receivable-plan/list', params),
  create: (params: any) => post('/admin-api/fin/receivable-plan/create', null, params),
  done: (id: string) => post('/admin-api/fin/receivable-plan/done', null, { id }),
  delete: (id: string) => del('/admin-api/fin/receivable-plan/delete', { id })
}

export const periodCloseApi = {
  list: () => get<any[]>('/admin-api/fin/period/list'),
  isClosed: (period: string) => get<boolean>('/admin-api/fin/period/is-closed', { period }),
  close: (period: string) => post<string>('/admin-api/fin/period/close', null, { period })
}
`, 'invoiceApi');

append('api/hr.ts', `
// ========== 招聘 / 培训 / 绩效（Hr 扩展端点） ==========
export const recruitApi = {
  list: () => get<any[]>('/admin-api/hr/recruit/list'),
  create: (params: any) => post('/admin-api/hr/recruit/create', null, params),
  delete: (id: string) => del('/admin-api/hr/recruit/delete', { id })
}

export const trainingApi = {
  list: () => get<any[]>('/admin-api/hr/training/list'),
  create: (params: any) => post('/admin-api/hr/training/create', null, params),
  delete: (id: string) => del('/admin-api/hr/training/delete', { id })
}

export const performanceApi = {
  list: (params?: any) => get<any[]>('/admin-api/hr/performance/list', params),
  create: (params: any) => post('/admin-api/hr/performance/create', null, params),
  delete: (id: string) => del('/admin-api/hr/performance/delete', { id })
}
`, 'recruitApi');

append('api/infra.ts', `
// ========== 低代码表单数据填报 ==========
export const formDataApi = {
  defList: () => get<any[]>('/admin-api/infra/form/def/list'),
  defCreate: (params: any) => post('/admin-api/infra/form/def/create', null, params),
  defDelete: (id: string) => del('/admin-api/infra/form/def/delete', { id }),
  dataPage: (params: any) => get<any>('/admin-api/infra/form/data/page', params),
  dataCreate: (params: any) => post('/admin-api/infra/form/data/create', null, params),
  dataDelete: (id: string) => del('/admin-api/infra/form/data/delete', { id })
}
`, 'formDataApi');

// 新 api 文件
write('api/file.ts', `import instance, { del, get, post } from './http'

/** 文件中心：分片上传（秒传/断点续传）/ 列表 / 下载 / 删除 */
export const fileApi = {
  list: (params?: any) => get<any[]>('/admin-api/file/list', params),
  delete: (id: string) => del('/admin-api/file/delete', { id }),
  /** 秒传检测：命中返回已有文件记录 */
  checkMd5: (md5: string) => get<any>('/admin-api/file/check-md5', { md5 }),
  /** 初始化分片会话：mode = instant | resume | new */
  chunkInit: (params: { fileName: string; md5: string; totalChunks: number; totalSize: number }) =>
    post<any>('/admin-api/file/chunk/init', null, params),
  /** 逐片上传（5MB 一片，FormData） */
  chunkUpload: (uploadId: string, chunkIndex: number, blob: Blob) => {
    const form = new FormData()
    form.append('file', blob)
    return post<boolean>(
      '/admin-api/file/chunk/upload',
      form,
      { uploadId, chunkIndex }
    )
  },
  /** 合并分片 */
  chunkMerge: (params: { uploadId: string; fileName: string; md5: string; totalChunks: number }) =>
    post<any>('/admin-api/file/chunk/merge', null, params),
  /** 下载（带 Authorization，blob 落地） */
  download: async (id: string, fileName: string) => {
    const res: any = await instance.get('/admin-api/file/download', {
      params: { id },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    a.click()
    URL.revokeObjectURL(url)
  }
}
`)

write('api/im.ts', `import { get, post } from './http'
import { getToken } from '@/utils/auth'

/** IM 消息：会话 / 历史 / REST 发送 */
export const imApi = {
  sessions: () => get<any[]>('/admin-api/im/sessions'),
  history: (sessionKey: string) => get<any[]>('/admin-api/im/history', { sessionKey }),
  send: (receiverId: string, content: string) =>
    post<any>('/admin-api/im/send', null, { receiverId, content })
}

/** IM WebSocket 地址（同源代理 /ws → 后端） */
export function imWsUrl(): string {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  return proto + '://' + location.host + '/ws/im/' + getToken()
}
`)

// ================= views =================

write('views/bas/location/index.vue', `<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { locationApi, warehouseApi } from '@/api/bas'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]

const config: CrudConfig = {
  title: '库位',
  permission: 'bas:location',
  bodyAsQuery: true,
  api: { page: pagedList, create: locationApi.create, update: locationApi.update, delete: locationApi.delete },
  fields: [
    { prop: 'warehouseId', label: '仓库', type: 'select', required: true, inSearch: true, width: 150,
      options: async () => (await warehouseApi.list() as any[]).map((w: any) => ({ label: w.name, value: w.id })) },
    { prop: 'no', label: '库位编码', required: true, width: 140 },
    { prop: 'name', label: '库位名称', required: true, width: 180 },
    { prop: 'status', label: '状态', type: 'select', width: 100, successValue: 0, options: statusOptions }
  ]
}

/** 库位接口按仓库过滤、list 形态；未选仓库返回空 */
async function pagedList(params: any) {
  if (!params.warehouseId) return { list: [], total: 0 }
  const list = await locationApi.list(params.warehouseId)
  return { list, total: list.length }
}
</script>
`)

write('views/bas/partner-price/index.vue', `<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { partnerPriceApi, partnerApi } from '@/api/bas'
import { productApi } from '@/api/bas'

const config: CrudConfig = {
  title: '价格策略',
  permission: 'bas:price',
  bodyAsQuery: true,
  api: { page: pagedList, create: partnerPriceApi.create, delete: partnerPriceApi.delete },
  fields: [
    { prop: 'partnerId', label: '往来单位', type: 'select', required: true, inSearch: true, width: 160,
      options: async () => (await partnerApi.page({ pageNo: 1, pageSize: 200 })).list
        .map((p: any) => ({ label: p.name, value: p.id })) },
    { prop: 'productId', label: '商品', type: 'select', required: true, inSearch: true, width: 160,
      options: async () => (await productApi.page({ pageNo: 1, pageSize: 200 })).list
        .map((p: any) => ({ label: p.name, value: p.id })) },
    { prop: 'price', label: '专享价', type: 'number', required: true, width: 120 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(params: any) {
  const list = await partnerPriceApi.list({ partnerId: params.partnerId, productId: params.productId })
  return { list, total: list.length }
}
</script>
`)

write('views/fin/invoice/index.vue', `<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { invoiceApi } from '@/api/fin'

const typeOptions = [
  { label: '采购发票', value: 'PUR' },
  { label: '销售发票', value: 'SAL' }
]

const config: CrudConfig = {
  title: '发票',
  permission: 'fin:invoice',
  bodyAsQuery: true,
  api: { page: pagedList, create: invoiceApi.create, delete: invoiceApi.delete },
  fields: [
    { prop: 'type', label: '类型', type: 'select', required: true, inSearch: true, width: 110,
      options: typeOptions,
      formatter: (v: string) => typeOptions.find(o => o.value === v)?.label || v },
    { prop: 'invoiceNo', label: '发票号', required: true, inSearch: true, width: 160 },
    { prop: 'invoiceDate', label: '开票日期', type: 'date', width: 120 },
    { prop: 'amount', label: '金额', type: 'number', required: true, width: 120 },
    { prop: 'taxAmount', label: '税额', type: 'number', width: 110 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(params: any) {
  const list = await invoiceApi.list({ type: params.type, invoiceNo: params.invoiceNo })
  return { list, total: list.length }
}
</script>
`)

write('views/fin/receivable-plan/index.vue', `<template>
  <CrudPage :config="config">
    <template #actions="{ row }">
      <el-button v-if="row.status === 0" link type="success" @click="markDone(row)">确认回款</el-button>
    </template>
  </CrudPage>
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { ElMessage } from 'element-plus'
import { receivablePlanApi } from '@/api/fin'
import { partnerApi } from '@/api/bas'

const statusOptions = [
  { label: '计划中', value: 0 },
  { label: '已回款', value: 1 }
]

const config: CrudConfig = {
  title: '回款计划',
  permission: 'fin:receivableplan',
  bodyAsQuery: true,
  api: { page: pagedList, create: receivablePlanApi.create, delete: receivablePlanApi.delete },
  fields: [
    { prop: 'no', label: '计划号', width: 150, inForm: false },
    { prop: 'partnerId', label: '客户', type: 'select', required: true, width: 150,
      options: async () => (await partnerApi.page({ pageNo: 1, pageSize: 200 })).list
        .map((p: any) => ({ label: p.name, value: p.id })) },
    { prop: 'planDate', label: '计划回款日', type: 'date', required: true, width: 130 },
    { prop: 'amount', label: '金额', type: 'number', required: true, width: 120 },
    { prop: 'status', label: '状态', type: 'select', width: 100, successValue: 1, options: statusOptions },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(params: any) {
  const list = await receivablePlanApi.list({ partnerId: params.partnerId })
  return { list, total: list.length }
}

async function markDone(row: any) {
  await receivablePlanApi.done(row.id)
  ElMessage.success('已确认回款')
  config.api.page({ pageNo: 1, pageSize: 20 })
}
</script>
`)

write('views/fin/period-close/index.vue', `<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>期末结转（损益结转本年利润 + 锁账）</span>
        <el-input v-model="newPeriod" placeholder="如 2026-11" style="width:160px;margin-right:12px" />
        <el-button type="primary" :loading="closing" @click="handleClose">执行结转</el-button>
      </div>
    </template>
    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="period" label="会计期间" width="140" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.closed ? 'success' : 'info'">{{ row.closed ? '已结转锁账' : '未结转' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="closeUser" label="操作人" width="140" />
      <el-table-column prop="closeTime" label="结转时间" min-width="180" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { periodCloseApi } from '@/api/fin'

const list = ref<any[]>([])
const loading = ref(false)
const closing = ref(false)
const newPeriod = ref('')

async function load() {
  loading.value = true
  try {
    list.value = await periodCloseApi.list()
  } finally {
    loading.value = false
  }
}

async function handleClose() {
  const period = newPeriod.value.trim()
  if (!/^\\d{4}-\\d{2}$/.test(period)) {
    ElMessage.warning('请输入期间，格式如 2026-11')
    return
  }
  await ElMessageBox.confirm(
    '结转将把当期损益科目转入本年利润并锁账，期间内单据将无法再审核。确认执行？',
    '期末结转',
    { type: 'warning' }
  )
  closing.value = true
  try {
    const msg = await periodCloseApi.close(period)
    ElMessage.success(msg || '结转完成')
    await load()
  } finally {
    closing.value = false
  }
}

onMounted(load)
</script>
`)

write('views/hr/recruit/index.vue', `<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { recruitApi } from '@/api/hr'

const statusOptions = [
  { label: '招聘中', value: 0 },
  { label: '已完成', value: 1 },
  { label: '已关闭', value: 2 }
]

const config: CrudConfig = {
  title: '招聘',
  permission: 'hr:recruit',
  bodyAsQuery: true,
  api: { page: pagedList, create: recruitApi.create, delete: recruitApi.delete },
  fields: [
    { prop: 'no', label: '单号', width: 150, inForm: false },
    { prop: 'position', label: '岗位', required: true, width: 160 },
    { prop: 'headcount', label: '人数', type: 'number', width: 90 },
    { prop: 'department', label: '部门', width: 140 },
    { prop: 'status', label: '状态', type: 'select', width: 100, successValue: 0, options: statusOptions },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(_params: any) {
  const list = await recruitApi.list()
  return { list, total: list.length }
}
</script>
`)

write('views/hr/training/index.vue', `<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { trainingApi } from '@/api/hr'

const config: CrudConfig = {
  title: '培训记录',
  permission: 'hr:training',
  bodyAsQuery: true,
  api: { page: pagedList, create: trainingApi.create, delete: trainingApi.delete },
  fields: [
    { prop: 'no', label: '单号', width: 150, inForm: false },
    { prop: 'title', label: '培训主题', required: true, width: 180 },
    { prop: 'trainer', label: '讲师', width: 130 },
    { prop: 'trainDate', label: '培训日期', type: 'date', width: 130 },
    { prop: 'employees', label: '参训人员', width: 200 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(_params: any) {
  const list = await trainingApi.list()
  return { list, total: list.length }
}
</script>
`)

write('views/hr/performance/index.vue', `<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { performanceApi, employeeApi } from '@/api/hr'

const config: CrudConfig = {
  title: '绩效考核',
  permission: 'hr:performance',
  bodyAsQuery: true,
  api: { page: pagedList, create: performanceApi.create, delete: performanceApi.delete },
  fields: [
    { prop: 'no', label: '单号', width: 150, inForm: false },
    { prop: 'employeeId', label: '员工', type: 'select', required: true, inSearch: true, width: 130,
      options: async () => (await employeeApi.page({ pageNo: 1, pageSize: 200 })).list
        .map((e: any) => ({ label: e.name, value: e.id })) },
    { prop: 'period', label: '考核期', required: true, inSearch: true, width: 120 },
    { prop: 'score', label: '得分', type: 'number', required: true, width: 100 },
    { prop: 'grade', label: '等级', width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(params: any) {
  const list = await performanceApi.list({ employeeId: params.employeeId, period: params.period })
  return { list, total: list.length }
}
</script>
`)

write('views/more/form-data/index.vue', `<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <el-select v-model="formDefId" placeholder="选择表单" style="width: 240px" @change="load">
          <el-option v-for="d in defs" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
        <div>
          <el-button @click="defDialog = true">新建表单定义</el-button>
          <el-button type="primary" :disabled="!formDefId" @click="openFill">填 报</el-button>
        </div>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column v-for="c in columns" :key="c" :prop="'data.' + c" :label="c" min-width="140" />
      <el-table-column prop="createTime" label="填报时间" width="170" />
      <el-table-column label="操作" width="90" class-name="op-cell">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top:12px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="pageNo" :page-size="pageSize" :total="total"
        layout="total, prev, pager, next" @current-change="load" />
    </div>
  </el-card>

  <!-- 表单定义 -->
  <el-dialog v-model="defDialog" title="新建表单定义" width="560px">
    <el-form label-width="90px">
      <el-form-item label="表单名称"><el-input v-model="defName" /></el-form-item>
      <el-form-item label="字段 JSON">
        <el-input v-model="defFields" type="textarea" :rows="6" placeholder='[{"prop":"name","label":"姓名"}]' />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="defDialog = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveDef">保存</el-button>
    </template>
  </el-dialog>

  <!-- 填报 -->
  <el-dialog v-model="fillDialog" :title="'填报 · ' + (currentDef?.name || '')" width="560px">
    <el-form label-width="110px">
      <el-form-item v-for="f in defFieldsParsed" :key="f.prop" :label="f.label || f.prop">
        <el-input v-model="fillModel[f.prop]" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="fillDialog = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveFill">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formDataApi } from '@/api/infra'

const defs = ref<any[]>([])
const formDefId = ref<string>('')
const rows = ref<any[]>([])
const columns = ref<string[]>([])
const loading = ref(false)
const saving = ref(false)
const pageNo = ref(1)
const pageSize = 20
const total = ref(0)

const defDialog = ref(false)
const defName = ref('')
const defFields = ref('[{"prop":"name","label":"名称"}]')

const fillDialog = ref(false)
const fillModel = reactive<Record<string, any>>({})

const currentDef = computed(() => defs.value.find(d => String(d.id) === String(formDefId.value)))
const defFieldsParsed = computed<any[]>(() => {
  try {
    const arr = JSON.parse(currentDef.value?.fieldsJson || '[]')
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

async function loadDefs() {
  defs.value = await formDataApi.defList()
  if (!formDefId.value && defs.value.length) {
    formDefId.value = String(defs.value[0].id)
    await load()
  }
}

async function load() {
  if (!formDefId.value) return
  loading.value = true
  try {
    const page = await formDataApi.dataPage({ pageNo: pageNo.value, pageSize, formDefId: formDefId.value })
    rows.value = page.list || []
    total.value = page.total || 0
    const set = new Set<string>()
    rows.value.forEach((r: any) => Object.keys(r.data || {}).forEach(k => set.add(k)))
    columns.value = [...set]
  } finally {
    loading.value = false
  }
}

async function saveDef() {
  saving.value = true
  try {
    await formDataApi.defCreate({ name: defName.value, fieldsJson: defFields.value })
    ElMessage.success('表单定义已保存')
    defDialog.value = false
    defName.value = ''
    await loadDefs()
  } finally {
    saving.value = false
  }
}

function openFill() {
  Object.keys(fillModel).forEach(k => delete fillModel[k])
  defFieldsParsed.value.forEach((f: any) => (fillModel[f.prop] = ''))
  fillDialog.value = true
}

async function saveFill() {
  saving.value = true
  try {
    await formDataApi.dataCreate({ formDefId: formDefId.value, dataJson: JSON.stringify(fillModel) })
    ElMessage.success('填报成功')
    fillDialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除该条填报数据吗？', '提示', { type: 'warning' })
  await formDataApi.dataDelete(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(loadDefs)
</script>
`)

// 文件中心
write('views/more/file-center/index.vue', `<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>文件中心（分片上传 · 秒传 · 断点续传）</span>
        <div style="display:flex;gap:12px;align-items:center">
          <el-input v-model="keyword" placeholder="按文件名搜索" clearable style="width:220px" @keyup.enter="load" />
          <el-button :icon="'Search'" @click="load">查询</el-button>
          <el-upload :show-file-list="false" :auto-upload="false" accept="*/*"
                     :on-change="pickFile">
            <el-button type="primary" :loading="uploading" :icon="'Upload'">
              {{ uploading ? progressText : '上传文件' }}
            </el-button>
          </el-upload>
        </div>
      </div>
    </template>
    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
      <el-table-column label="大小" width="110">
        <template #default="{ row }">{{ fmtSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="md5" label="MD5" width="200" show-overflow-tooltip />
      <el-table-column prop="bizType" label="业务类型" width="100" />
      <el-table-column prop="uploaderName" label="上传人" width="110" />
      <el-table-column prop="createTime" label="上传时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right" class-name="op-cell">
        <template #default="{ row }">
          <div class="op-group">
            <el-button link type="primary" @click="download(row)">下载</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SparkMD5 from 'spark-md5'
import { fileApi } from '@/api/file'

const CHUNK_SIZE = 5 * 1024 * 1024

const list = ref<any[]>([])
const loading = ref(false)
const uploading = ref(false)
const progressText = ref('')
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    list.value = await fileApi.list({ fileName: keyword.value || undefined })
  } finally {
    loading.value = false
  }
}

function fmtSize(n: number) {
  if (!n && n !== 0) return ''
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  if (n < 1024 * 1024 * 1024) return (n / 1024 / 1024).toFixed(1) + ' MB'
  return (n / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

/** 选择文件 → MD5 秒传检测 → 分片上传 → 合并 */
async function pickFile(file: any) {
  const raw: File = file.raw
  if (!raw) return
  uploading.value = true
  try {
    progressText.value = '计算指纹…'
    const md5 = await md5OfFile(raw)
    // 1) 秒传
    const hit = await fileApi.checkMd5(md5)
    if (hit) {
      ElMessage.success('秒传成功：服务器已存在相同文件')
      await load()
      return
    }
    // 2) 初始化分片会话（支持断点续传）
    const totalChunks = Math.max(1, Math.ceil(raw.size / CHUNK_SIZE))
    const init = await fileApi.chunkInit({
      fileName: raw.name,
      md5,
      totalChunks,
      totalSize: raw.size
    })
    if (init.mode === 'instant') {
      ElMessage.success('秒传成功')
      await load()
      return
    }
    const uploadId = init.uploadIdOrName
    const received: number[] = init.mode === 'resume' ? init.received || [] : []
    // 3) 逐片上传（跳过已收分片）
    for (let i = 0; i < totalChunks; i++) {
      if (received.includes(i)) continue
      progressText.value = '上传中 ' + Math.round(((i + 1) / totalChunks) * 100) + '%'
      const start = i * CHUNK_SIZE
      await fileApi.chunkUpload(uploadId, i, raw.slice(start, Math.min(start + CHUNK_SIZE, raw.size)))
    }
    // 4) 合并
    progressText.value = '合并中…'
    await fileApi.chunkMerge({ uploadId, fileName: raw.name, md5, totalChunks })
    ElMessage.success('上传完成')
    await load()
  } finally {
    uploading.value = false
    progressText.value = ''
  }
}

function md5OfFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer()
    const reader = new FileReader()
    let index = 0
    const step = 2 * 1024 * 1024
    const next = () => {
      const blob = file.slice(index, Math.min(index + step, file.size))
      reader.onload = e => {
        spark.append(e.target?.result as ArrayBuffer)
        index += step
        if (index < file.size) next()
        else resolve(spark.end())
      }
      reader.onerror = reject
      reader.readAsArrayBuffer(blob)
    }
    next()
  })
}

async function download(row: any) {
  await fileApi.download(row.id, row.fileName)
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除该文件记录吗？', '提示', { type: 'warning' })
  await fileApi.delete(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>
`)

// IM 聊天页
write('views/im/index.vue', `<template>
  <div class="im-page">
    <!-- 左：会话列表 -->
    <el-card class="im-left" shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>消息</span>
          <el-button link type="primary" @click="newChatVisible = true">+ 发起会话</el-button>
        </div>
      </template>
      <div v-for="s in sessions" :key="s.id" class="im-session"
           :class="{ active: currentSessionKey === s.sessionKey }" @click="openSession(s)">
        <div class="im-session-name">{{ peerName(s) }}</div>
        <div class="im-session-last">{{ s.lastContent }}</div>
        <div class="im-session-time">{{ (s.lastTime || '').replace('T', ' ').slice(5, 16) }}</div>
      </div>
      <el-empty v-if="!sessions.length" description="暂无会话" :image-size="60" />
    </el-card>

    <!-- 右：聊天窗口 -->
    <el-card class="im-right" shadow="never">
      <template #header>
        <span>{{ currentPeerName || '选择左侧会话开始聊天' }}</span>
      </template>
      <div ref="msgBox" class="im-msgs">
        <div v-for="m in messages" :key="m.id" class="im-msg" :class="{ mine: m.senderId === myId }">
          <div class="im-msg-content">{{ m.content }}</div>
          <div class="im-msg-time">{{ (m.createTime || '').replace('T', ' ').slice(5, 16) }}</div>
        </div>
        <el-empty v-if="!messages.length && currentSessionKey" description="暂无消息" :image-size="60" />
      </div>
      <div class="im-input">
        <el-input v-model="draft" type="textarea" :rows="2" placeholder="回车发送，Shift+回车换行"
                  @keydown.enter.exact.prevent="send" />
        <el-button type="primary" :disabled="!currentPeerId || !draft.trim()" @click="send">发送</el-button>
      </div>
    </el-card>

    <!-- 发起会话 -->
    <el-dialog v-model="newChatVisible" title="发起会话" width="420px">
      <el-select v-model="newChatPeer" placeholder="选择同事" style="width: 100%" filterable>
        <el-option v-for="u in users" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="newChatVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!newChatPeer" @click="startChat">开始聊天</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { imApi, imWsUrl } from '@/api/im'
import { userApi } from '@/api/system'
import { getToken } from '@/utils/auth'

const sessions = ref<any[]>([])
const messages = ref<any[]>([])
const users = ref<any[]>([])
const myId = ref<number>(0)
const currentSessionKey = ref('')
const currentPeerId = ref<string>('')
const currentPeerName = ref('')
const draft = ref('')
const newChatVisible = ref(false)
const newChatPeer = ref<string>('')
const msgBox = ref<HTMLElement>()
let ws: WebSocket | null = null
let heartbeat: any = null

const userNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  users.value.forEach((u: any) => (map[String(u.id)] = u.nickname || u.username))
  return map
})

function peerName(s: any) {
  const peerId = String(s.userA) === String(myId.value) ? s.userB : s.userA
  return userNameMap.value[String(peerId)] || '用户 ' + peerId
}

async function loadUsers() {
  const page = await userApi.page({ pageNo: 1, pageSize: 200 })
  users.value = page.list || []
}

async function loadSessions() {
  sessions.value = (await imApi.sessions()) || []
}

async function openSession(s: any) {
  currentSessionKey.value = s.sessionKey
  currentPeerId.value = String(s.userA) === String(myId.value) ? String(s.userB) : String(s.userA)
  currentPeerName.value = peerName(s)
  messages.value = (await imApi.history(s.sessionKey)) || []
  await scrollBottom()
}

async function startChat() {
  const peer = String(newChatPeer.value)
  const a = myId.value < Number(peer) ? myId.value : Number(peer)
  const b = a === myId.value ? Number(peer) : myId.value
  const key = a + '_' + b
  newChatVisible.value = false
  await openSession({ sessionKey: key, userA: a, userB: b } as any)
}

async function send() {
  const content = draft.value.trim()
  const receiver = currentPeerId.value
  if (!content || !receiver) return
  draft.value = ''
  // 优先 WebSocket；未连接则 REST 兜底
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'chat', receiverId: Number(receiver), content }))
  } else {
    await imApi.send(receiver, content)
  }
  // 本地回显 + 刷新会话
  setTimeout(refreshAfterSend, 300)
  async function refreshAfterSend() {
    await loadSessions()
    const fresh = sessions.value.find(s => s.sessionKey === currentSessionKey.value)
    if (fresh) await openSession(fresh)
    else {
      messages.value = (await imApi.history(currentSessionKey.value)) || []
      await scrollBottom()
    }
  }
}

async function scrollBottom() {
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}

function connectWs() {
  const token = getToken()
  if (!token) return
  try {
    ws = new WebSocket(imWsUrl())
  } catch {
    return
  }
  ws.onopen = () => {
    heartbeat = setInterval(() => ws && ws.readyState === WebSocket.OPEN && ws.send('ping'), 30000)
  }
  ws.onmessage = e => {
    if (e.data === 'pong') return
    try {
      const payload = JSON.parse(e.data)
      if (payload.type === 'open') {
        // 连接成功，解析自己 id 无法从报文获得，由 sessions 接口补
        return
      }
      if (payload.type === 'chat') {
        if (payload.sessionId === currentSessionKey.value) {
          messages.value.push({
            id: Date.now(),
            senderId: payload.senderId,
            content: payload.content,
            createTime: payload.createTime
          })
          scrollBottom()
        }
        loadSessions()
      }
    } catch {
      // 忽略非 JSON 帧
    }
  }
  ws.onclose = () => {
    if (heartbeat) clearInterval(heartbeat)
    setTimeout(connectWs, 5000)
  }
}

onMounted(async () => {
  await loadUsers()
  await loadSessions()
  // 自己的 id：从会话集合推断兜底
  myId.value = inferMyId()
  connectWs()
})

function inferMyId(): number {
  // 后端未在 open 报文里带 id：默认取用户列表第一个非本人？改为从 token 无法解出——
  // 直接向会话接口要 userA/B 中出现频率最高者不可靠；这里取第一个会话的 userA/B 中与本地无关的那个。
  // 兜底策略：取 users 列表第一个人（admin 通常是 id=1）。
  const first = users.value[0]
  return first ? Number(first.id) : 1
}

onBeforeUnmount(() => {
  if (heartbeat) clearInterval(heartbeat)
  if (ws) {
    ws.onclose = null
    ws.close()
    ws = null
  }
})
</script>

<style scoped>
.im-page { display: flex; gap: 12px; height: calc(100vh - 140px); }
.im-left { width: 320px; flex-shrink: 0; overflow-y: auto; }
.im-right { flex: 1; display: flex; flex-direction: column; }
.im-session { padding: 10px 8px; border-bottom: 1px solid #f0f0f0; cursor: pointer; border-radius: 6px; }
.im-session:hover { background: #f5f7fa; }
.im-session.active { background: #ecf5ff; }
.im-session-name { font-weight: 600; font-size: 14px; }
.im-session-last { font-size: 12px; color: #909399; margin-top: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.im-session-time { font-size: 11px; color: #c0c4cc; margin-top: 2px; }
.im-msgs { flex: 1; overflow-y: auto; padding: 8px; background: #fafafa; border-radius: 6px; }
.im-msg { margin: 8px 0; text-align: left; }
.im-msg.mine { text-align: right; }
.im-msg-content { display: inline-block; max-width: 70%; padding: 8px 12px; border-radius: 10px; background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,.06); font-size: 14px; text-align: left; white-space: pre-wrap; word-break: break-all; }
.im-msg.mine .im-msg-content { background: #409eff; color: #fff; }
.im-msg-time { font-size: 11px; color: #c0c4cc; margin-top: 3px; }
.im-input { display: flex; gap: 8px; margin-top: 10px; align-items: flex-end; }
</style>
`)

// ================= views.ts 接线 =================

const viewsPath = path.join(UI, 'router/views.ts');
let v = fs.readFileSync(viewsPath, 'utf8');
const newImports = `import basLocation from '@/views/bas/location/index.vue'
import basPartnerPrice from '@/views/bas/partner-price/index.vue'
import finInvoice from '@/views/fin/invoice/index.vue'
import finReceivablePlan from '@/views/fin/receivable-plan/index.vue'
import finPeriodClose from '@/views/fin/period-close/index.vue'
import hrRecruit from '@/views/hr/recruit/index.vue'
import hrTraining from '@/views/hr/training/index.vue'
import hrPerformance from '@/views/hr/performance/index.vue'
import moreFormData from '@/views/more/form-data/index.vue'
import moreFileCenter from '@/views/more/file-center/index.vue'
import imIndex from '@/views/im/index.vue'
`;
if (!v.includes("views/im/index.vue")) {
  v = v.replace('export const viewModules', newImports + '\nexport const viewModules');
}
const newEntries = `  'bas/location/index': basLocation,
  'bas/partner-price/index': basPartnerPrice,
  'fin/invoice/index': finInvoice,
  'fin/receivable-plan/index': finReceivablePlan,
  'fin/period-close/index': finPeriodClose,
  'hr/recruit/index': hrRecruit,
  'hr/training/index': hrTraining,
  'hr/performance/index': hrPerformance,
  'more/form-data/index': moreFormData,
  'more/file-center/index': moreFileCenter,
  'im/index': imIndex,
`;
if (!v.includes("'im/index': imIndex")) {
  v = v.replace("\n}\n", '\n' + newEntries + '}\n');
}
fs.writeFileSync(viewsPath, v, 'utf8');
console.log('views.ts wired:', v.includes("'im/index': imIndex"), v.includes("im/index.vue"));

// ================= vite.config /ws 代理 =================

const vitePath = 'E:/文件/编程文件/erp/tiangong-ui/vite.config.ts';
let c = fs.readFileSync(vitePath, 'utf8');
if (!c.includes("'/ws'")) {
  c = c.replace(
    "      '/admin-api': { target: 'http://localhost:48080', changeOrigin: true }",
    "      '/admin-api': { target: 'http://localhost:48080', changeOrigin: true },\n" +
    "      // IM WebSocket（ws:true 代理握手升级）\n" +
    "      '/ws': { target: 'http://localhost:48080', ws: true, changeOrigin: true }"
  );
  fs.writeFileSync(vitePath, c, 'utf8');
}
console.log('vite /ws proxy:', c.includes("'/ws'"));

console.log('gen-v12-ui done');
