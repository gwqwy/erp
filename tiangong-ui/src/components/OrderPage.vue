<template>
  <div class="page-card">
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form inline class="search-form" @submit.prevent>
        <el-form-item label="单号">
          <el-input v-model="searchNo" placeholder="单号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 工具栏 -->
      <div class="table-toolbar">
        <span style="font-weight: 600">{{ config.title }}</span>
        <el-button v-perm="config.permission + ':create'" type="primary" :icon="Plus" @click="openCreate">
          新增
        </el-button>
      </div>

      <!-- 列表 -->
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column v-for="f in tableFields" :key="f.prop" :label="f.label" :width="f.width">
          <template #default="{ row }">
            <template v-if="f.prop === 'status'">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
            <span v-else>{{ cellLabel(f, row[f.prop]) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="150" class-name="op-cell">
          <template #default="{ row }">
            <div class="op-group">
            <el-button link type="primary" @click="openView(row)">详情</el-button>
            <el-dropdown v-if="hasMoreActions(row)" trigger="click" placement="bottom-end"
                         @command="cmd => onMoreCommand(cmd, row)">
              <span class="op-more" @click.stop>
                更多<el-icon class="op-more-icon"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <slot name="actions" :row="row" />
                  <el-dropdown-item command="attach">附件</el-dropdown-item>
                  <el-dropdown-item v-if="config.deposit" command="deposit">订金</el-dropdown-item>
                  <el-dropdown-item command="print">打印</el-dropdown-item>
                  <el-dropdown-item v-if="canEdit(row) && config.api.update" v-perm="config.permission + ':update'"
                                    command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item v-if="config.api.audit && isDraft(row)" v-perm="config.permission + ':audit'"
                                    command="audit">审核</el-dropdown-item>
                  <el-dropdown-item v-if="config.api.submitApproval && isDraft(row)" v-perm="config.permission + ':submit'"
                                    command="submit">送审</el-dropdown-item>
                  <el-dropdown-item v-if="config.api.voidOrder && isDraft(row)" v-perm="config.permission + ':void'"
                                    command="void">作废</el-dropdown-item>
                  <el-dropdown-item v-if="config.api.reverse && config.reversible && isAudited(row)"
                                    v-perm="config.permission + ':reverse'" command="reverse">冲销</el-dropdown-item>
                  <el-dropdown-item v-if="isDraft(row)" v-perm="config.permission + ':delete'"
                                    command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 12px; justify-content: flex-end"
        @change="loadList"
      />

      <!-- 单据抽屉：创建 / 编辑 / 详情 -->
      <el-drawer v-model="dialogVisible" :title="dialogTitle" size="72%">
        <!-- 表头 -->
        <el-form :model="head" label-width="90px" :disabled="mode === 'view'">
          <el-row>
            <el-col v-for="f in config.masterFields" :key="f.prop" :span="f.span ?? 8">
              <el-form-item :label="f.label" :required="f.required && mode !== 'view'">
                <el-select v-if="f.type === 'partner-select'" v-model="head[f.prop]" filterable
                           style="width: 100%" :disabled="mode === 'view'">
                  <el-option v-for="o in partnerOptions(f.partnerType || 'customer')" :key="o.value"
                             :label="o.label" :value="o.value" />
                </el-select>
                <el-select v-else-if="f.type === 'warehouse-select'" v-model="head[f.prop]" filterable
                           style="width: 100%">
                  <el-option v-for="o in warehouses" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
                <el-select v-else-if="f.type === 'source-order'" v-model="head[f.prop]" filterable
                           style="width: 100%" clearable placeholder="选择来源单据（可空）"
                           @change="onSourceChange">
                  <el-option v-for="o in sourceOrders" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
                <el-select v-else-if="f.type === 'select'" v-model="head[f.prop]" style="width: 100%" clearable>
                  <el-option v-for="o in masterOptions(f)" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
                <el-date-picker v-else-if="f.type === 'date'" v-model="head[f.prop]" type="date"
                                value-format="YYYY-MM-DD" style="width: 100%" />
                <el-input v-else-if="f.type === 'textarea'" v-model="head[f.prop]" type="textarea" :rows="2" />
                <el-input v-else v-model="head[f.prop]" :placeholder="f.prop === 'no' ? '保存后自动生成' : ''"
                          :disabled="f.prop === 'no'" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <!-- 明细表 -->
        <div class="items-header">
          <span>单据明细</span>
          <el-button v-if="mode !== 'view'" type="primary" size="small" :icon="Plus" @click="addItem">添加行</el-button>
        </div>
        <el-table :data="items" border size="small">
          <el-table-column type="index" label="#" width="44" />
          <el-table-column v-for="f in config.itemFields" :key="f.prop" :label="f.label" :width="f.width">
            <template #default="{ row }">
              <el-select v-if="f.type === 'product-select' && mode !== 'view'" v-model="row[f.prop]"
                         filterable style="width: 100%" @change="pid => onProductChange(row, pid)">
                <el-option v-for="p in products" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
              <el-select v-else-if="f.type === 'select' && mode !== 'view'" v-model="row[f.prop]"
                         filterable style="width: 100%"
                         @change="v => onItemOptionChange(row, v, f)">
                <el-option v-for="o in itemOptions(f)" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-input-number v-else-if="f.type === 'number' && mode !== 'view' && !f.readonly"
                               v-model="row[f.prop]" :precision="f.precision ?? 2" :controls="false"
                               style="width: 100%" />
              <span v-else>{{ itemCellLabel(f, row) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="mode !== 'view'" label="操作" width="70">
            <template #default="{ $index }">
              <el-button link type="danger" @click="items.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="items-total">
          合计：数量 {{ totalQty }} ，金额 {{ totalAmount }}
        </div>

        <template v-if="mode === 'view' && currentId">
          <BizChatter :biz-type="config.bizType" :biz-id="currentId" />
        </template>

        <template v-if="mode !== 'view'">
          <div style="margin-top: 16px; text-align: right">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="handleSave">保存草稿</el-button>
          </div>
        </template>
      </el-drawer>

      <!-- 单据附件对话框 -->
      <el-dialog v-model="attachVisible" :title="'单据附件 · ' + config.title" width="640px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
          <span style="color:#909399;font-size:13px">附件与该单据关联，可在文件中心按业务类型检索</span>
          <el-upload :show-file-list="false" :auto-upload="false" :on-change="onAttachPick">
            <el-button type="primary" size="small" :loading="attachUploading">上传附件</el-button>
          </el-upload>
        </div>
        <el-table :data="attachRows" v-loading="attachUploading" size="small" border>
          <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
          <el-table-column label="大小" width="90">
            <template #default="{ row }">{{ fmtAttachSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="uploaderName" label="上传人" width="90" />
          <el-table-column label="操作" width="130" class-name="op-cell">
            <template #default="{ row }">
              <div class="op-group">
                <el-button link type="primary" @click="fileApi.download(row.id, row.fileName)">下载</el-button>
                <el-button link type="danger" @click="removeAttach(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>

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
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import BizChatter from './BizChatter.vue'
import { partnerApi, productApi, warehouseApi } from '@/api/bas'
import { fileApi } from '@/api/file'

export interface OrderMasterField {
  prop: string
  label: string
  type: 'input' | 'textarea' | 'date' | 'partner-select' | 'warehouse-select' | 'source-order' | 'select'
  required?: boolean
  span?: number
  width?: number | string
  inTable?: boolean
  partnerType?: 'supplier' | 'customer'
  /** type=select 时的异步选项 */
  options?: () => Promise<Array<{ label: string; value: any; raw?: any }>>
}

export interface OrderItemField {
  prop: string
  label: string
  type: 'product-select' | 'number' | 'input' | 'select'
  width?: number
  precision?: number
  readonly?: boolean
  /** 选产品后自动填充：{ 目标字段: 产品字段 } */
  autofill?: Record<string, string>
  /** type=select 时的异步选项 */
  options?: () => Promise<Array<{ label: string; value: any; raw?: any }>>
}

export interface OrderConfig {
  title: string
  permission: string
  /** Chatter 的 bizType，如 pur_order */
  bizType: string
  api: {
    page: (params: any) => Promise<any>
    detail: (id: string) => Promise<any>
    create: (dataOrParams: any) => Promise<any>
    update?: (dataOrParams: any) => Promise<any>
    delete: (id: string) => Promise<any>
    /** 无审批流的配置类单据（如 BOM）可不提供 audit/void */
    audit?: (id: string) => Promise<any>
    voidOrder?: (id: string) => Promise<any>
    /** 提交多级审批（配置了审批流的单据） */
    submitApproval?: (id: string) => Promise<any>
    /** 红字冲销（仅入库/出库/收付款单据提供） */
    reverse?: (id: string) => Promise<any>
  }
  /** 已审核单据是否显示冲销按钮（入库/出库类单据 true） */
  reversible?: boolean
  /** detail 响应中表头字段名：order / inbound / outbound */
  headKey: string
  /** 订金管理（销售/采购订单可选配置） */
  deposit?: {
    get: (id: string) => Promise<any>
    register: (id: string, amount: number) => Promise<any>
  }
  masterFields: OrderMasterField[]
  itemFields: OrderItemField[]
  /** 来源单据（入库引用采购订单等） */
  source?: {
    prop: string
    label: string
    pageApi: (params: any) => Promise<any>
    itemListApi: (orderId: string) => Promise<any[]>
    mapItem: (oi: any) => Record<string, any>
  }
}

const props = defineProps<{ config: OrderConfig }>()

const STATUS = [
  { value: 10, label: '草稿', tag: 'info' },
  { value: 20, label: '审批中', tag: 'warning' },
  { value: 30, label: '已审核', tag: 'success' },
  { value: 40, label: '已完成', tag: 'primary' },
  { value: 50, label: '已作废', tag: 'danger' },
  { value: 60, label: '已冲销', tag: 'danger' }
]
const DRAFT = 10

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const searchNo = ref('')
const searchStatus = ref<number | undefined>(undefined)

const dialogVisible = ref(false)
const mode = ref<'create' | 'edit' | 'view'>('create')
const currentId = ref<string>('')
const head = reactive<Record<string, any>>({})
const items = ref<Record<string, any>[]>([])

const products = ref<Array<{ label: string; value: string; raw: any }>>([])
const warehouses = ref<Array<{ label: string; value: string }>>([])
const partnerMap = ref<Record<string, Array<{ label: string; value: string }>>>({})
const sourceOrders = ref<Array<{ label: string; value: string }>>([])
const masterOptionStore = ref<Record<string, Array<{ label: string; value: any; raw?: any }>>>({})
const itemOptionStore = ref<Record<string, Array<{ label: string; value: any; raw?: any }>>>({})

function masterOptions(f: OrderMasterField) {
  return masterOptionStore.value[f.prop] ?? []
}
function itemOptions(f: OrderItemField) {
  return itemOptionStore.value[f.prop] ?? []
}

const tableFields = computed(() => props.config.masterFields.filter(f => f.inTable !== false))
const dialogTitle = computed(() => {
  const prefix = mode.value === 'create' ? '新增' : mode.value === 'edit' ? '编辑' : '详情'
  return `${prefix}${props.config.title}`
})
const totalQty = computed(() => items.value.reduce((s, r) => s + Number(r.qty || 0), 0))
const totalAmount = computed(() =>
  items.value.reduce((s, r) => s + Number(r.qty || 0) * Number(r.price || 0), 0).toFixed(2))

function statusLabel(v: number) {
  return STATUS.find(s => s.value === v)?.label ?? v
}
function statusTagType(v: number) {
  return (STATUS.find(s => s.value === v)?.tag || 'info') as any
}
function isDraft(row: any) {
  return row.status === DRAFT
}
function isAudited(row: any) {
  return Number(row.status) === 30
}
/** 该行是否有下拉可执行的操作（附件/打印恒可用，菜单不会为空） */
function hasMoreActions(row: any): boolean {
  if (isDraft(row)) return true
  if (config.api.audit && Number(row.status) === 20) return true // 审批中可继续审核
  if (config.api.reverse && config.reversible && Number(row.status) === 30) return true
  return true
}
function canEdit(row: any) {
  return isDraft(row) && !!props.config.api.update
}

function partnerOptions(type: 'supplier' | 'customer') {
  return partnerMap.value[type] ?? []
}

function cellLabel(f: { prop: string; type: string; partnerType?: string }, value: any) {
  if (value === null || value === undefined || value === '') return '—'
  if (f.type === 'partner-select') {
    const opts = partnerOptions(f.partnerType || 'customer')
    return opts.find(o => String(o.value) === String(value))?.label ?? value
  }
  if (f.type === 'warehouse-select') {
    return warehouses.value.find(o => String(o.value) === String(value))?.label ?? value
  }
  if (f.type === 'select') {
    const opts = masterOptionStore.value[f.prop] ?? []
    return opts.find(o => String(o.value) === String(value))?.label ?? value
  }
  return value
}

/** 明细单元格：金额实时计算、产品显示名称 */
function itemCellLabel(f: OrderItemField, row: any) {
  const v = row[f.prop]
  if (f.prop === 'amount') {
    return (Number(row.qty || 0) * Number(row.price || 0)).toFixed(2)
  }
  if (f.type === 'product-select') {
    if (v === null || v === undefined || v === '') return '—'
    return products.value.find(p => String(p.value) === String(v))?.label ?? v
  }
  if (f.type === 'select') {
    if (v === null || v === undefined || v === '') return '—'
    return itemOptions(f).find(o => String(o.value) === String(v))?.label ?? v
  }
  if (v === null || v === undefined || v === '') return '—'
  return v
}

/** 通用下拉选择后的 autofill：{ 目标字段: 选项 raw 上的字段 } */
function onItemOptionChange(row: any, value: any, f: OrderItemField) {
  const hit = itemOptions(f).find(o => String(o.value) === String(value))
  if (!hit?.raw || !f.autofill) return
  for (const [target, sourceField] of Object.entries(f.autofill)) {
    row[target] = hit.raw[sourceField] ?? undefined
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await props.config.api.page({
      pageNo: pageNo.value, pageSize: pageSize.value,
      no: searchNo.value || undefined, status: searchStatus.value
    })
    list.value = res?.list ?? []
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchNo.value = ''
  searchStatus.value = undefined
  pageNo.value = 1
  loadList()
}

function defaultItem(): Record<string, any> {
  const row: Record<string, any> = {}
  for (const f of props.config.itemFields) row[f.prop] = undefined
  return row
}

function openCreate() {
  mode.value = 'create'
  currentId.value = ''
  Object.keys(head).forEach(k => delete head[k])
  for (const f of props.config.masterFields) head[f.prop] = undefined
  items.value = [defaultItem()]
  dialogVisible.value = true
  loadSourceOrders()
}

async function openEdit(row: any) {
  mode.value = 'edit'
  currentId.value = row.id
  const detail = await props.config.api.detail(row.id)
  Object.keys(head).forEach(k => delete head[k])
  Object.assign(head, detail[props.config.headKey])
  items.value = detail.items.map((i: any) => ({ ...i }))
  dialogVisible.value = true
  loadSourceOrders()
}

async function openView(row: any) {
  mode.value = 'view'
  currentId.value = row.id
  const detail = await props.config.api.detail(row.id)
  Object.keys(head).forEach(k => delete head[k])
  Object.assign(head, detail[props.config.headKey])
  items.value = detail.items.map((i: any) => ({ ...i }))
  dialogVisible.value = true
}

async function handleSave() {
  // 校验
  for (const f of props.config.masterFields) {
    if (f.required && f.type !== 'source-order' && !head[f.prop]) {
      ElMessage.warning(`${f.label}不能为空`)
      return
    }
  }
  const validItems = items.value.filter(r => r.productId && Number(r.qty) > 0)
  if (!validItems.length) {
    ElMessage.warning('请至少填写一行有效明细（产品 + 数量）')
    return
  }
  const payload = {
    ...head,
    items: validItems.map(r => {
      const clean: Record<string, any> = {}
      for (const f of props.config.itemFields) clean[f.prop] = r[f.prop]
      // 保留来源单据关联字段（入库/出库引用订单行）
      if (r.orderItemId !== undefined && r.orderItemId !== null) {
        clean.orderItemId = r.orderItemId
      }
      clean.qty = Number(r.qty)
      clean.price = Number(r.price || 0)
      return clean
    })
  }
  saving.value = true
  try {
    if (mode.value === 'create') {
      await props.config.api.create(payload)
    } else {
      await props.config.api.update!({ id: currentId.value, ...payload })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleAudit(row: any) {
  await ElMessageBox.confirm(`确认审核该单据吗？审核后将执行库存过账等联动。`, '提示', { type: 'warning' })
  await props.config.api.audit!(row.id)
  ElMessage.success('审核成功')
  loadList()
}

async function handleSubmitApproval(row: any) {
  await ElMessageBox.confirm('提交到多级审批流？（未配置审批流会提示改用直接审核）', '提示', { type: 'warning' })
  await props.config.api.submitApproval!(row.id)
  ElMessage.success('已提交审批')
  loadList()
}

/** 更多▾ 下拉统一分发 */
function onMoreCommand(cmd: string, row: any) {
  switch (cmd) {
    case 'edit': openEdit(row); break
    case 'audit': handleAudit(row); break
    case 'submit': handleSubmitApproval(row); break
    case 'void': handleVoid(row); break
    case 'reverse': handleReverse(row); break
    case 'delete': handleDelete(row); break
    case 'attach': openAttach(row); break
    case 'deposit': openDeposit(row); break
    case 'print': window.open('/print/order?type=' + props.config.bizType + '&id=' + row.id, '_blank'); break
  }
}

// ========== 单据附件（复用文件中心，bizType+bizId 关联） ==========
const attachVisible = ref(false)
const attachBizId = ref('')
const attachRows = ref<any[]>([])
const attachUploading = ref(false)

function openAttach(row: any) {
  attachBizId.value = String(row.id)
  attachVisible.value = true
  loadAttach()
}

async function loadAttach() {
  attachRows.value = await fileApi.list({ bizType: props.config.bizType, bizId: attachBizId.value })
}

async function onAttachPick(file: any) {
  const raw: File = file.raw
  if (!raw) return
  attachUploading.value = true
  try {
    await fileApi.uploadTo(raw, props.config.bizType, attachBizId.value)
    ElMessage.success('附件已上传')
    await loadAttach()
  } finally {
    attachUploading.value = false
  }
}

async function removeAttach(row: any) {
  await ElMessageBox.confirm('确认删除该附件吗？', '提示', { type: 'warning' })
  await fileApi.delete(row.id)
  await loadAttach()
}

// ========== 订金（销售/采购订单） ==========
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

function fmtAttachSize(n: number) {
  if (!n && n !== 0) return ''
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}

async function handleReverse(row: any) {
  await ElMessageBox.confirm(
    '红字冲销：库存反向过账、应收应付作废、生成红字凭证、回退订单进度。已核销的单据不可冲销。确认执行？',
    '红字冲销', { type: 'warning', confirmButtonText: '确认冲销' })
  await props.config.api.reverse!(row.id)
  ElMessage.success('冲销完成')
  loadList()
}

async function handleVoid(row: any) {
  await ElMessageBox.confirm('确认作废该单据吗？', '提示', { type: 'warning' })
  await props.config.api.voidOrder(row.id)
  ElMessage.success('已作废')
  loadList()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除该草稿吗？', '提示', { type: 'warning' })
  await props.config.api.delete(row.id)
  ElMessage.success('删除成功')
  loadList()
}

function addItem() {
  items.value.push(defaultItem())
}

async function onProductChange(row: any, productId: string) {
  const hit = products.value.find(p => String(p.value) === String(productId))
  if (!hit) return
  // 按字段配置自动填充（如 unitId <- 产品默认单位、price <- 参考价）
  for (const f of props.config.itemFields) {
    if (!f.autofill) continue
    for (const [target, sourceField] of Object.entries(f.autofill)) {
      if (target === f.prop) continue
      row[target] = hit.raw[sourceField] ?? undefined
    }
  }
}

async function onSourceChange(orderId: string) {
  if (!props.config.source || !orderId) return
  const orderItems = await props.config.source.itemListApi(orderId)
  items.value = orderItems.map(oi => {
    const row = props.config.source!.mapItem(oi)
    return { ...defaultItem(), ...row }
  })
}

async function loadSourceOrders() {
  if (!props.config.source) return
  const res = await props.config.source.pageApi({ pageNo: 1, pageSize: 50, status: 30 })
  sourceOrders.value = (res?.list ?? []).map((o: any) => ({ label: `${o.no}（${statusLabel(o.status)}）`, value: o.id }))
}

onMounted(async () => {
  loadList()
  // 基础数据选项
  const pp = await productApi.page({ pageNo: 1, pageSize: 200, status: 0 })
  products.value = (pp?.list ?? []).map((p: any) => ({
    label: `${p.name}${p.spec ? ' / ' + p.spec : ''}`,
    value: p.id,
    raw: p
  }))
  warehouses.value = (await warehouseApi.list({ status: 0 })).map((w: any) => ({ label: w.name, value: w.id }))
  for (const type of ['supplier', 'customer'] as const) {
    const params: any = { pageNo: 1, pageSize: 200, status: 0 }
    if (type === 'supplier') params.isSupplier = true
    else params.isCustomer = true
    const res = await partnerApi.page(params)
    partnerMap.value[type] = (res?.list ?? []).map((p: any) => ({ label: p.name, value: p.id }))
  }
  // 通用下拉选项（表头 + 明细行）
  for (const f of props.config.masterFields) {
    if (f.type === 'select' && typeof f.options === 'function') {
      masterOptionStore.value[f.prop] = await f.options()
    }
  }
  for (const f of props.config.itemFields) {
    if (f.type === 'select' && typeof f.options === 'function') {
      itemOptionStore.value[f.prop] = await f.options()
    }
  }
})
</script>

<style scoped>
.items-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  margin: 8px 0;
}
.items-total {
  text-align: right;
  color: #606266;
  font-size: 13px;
  margin-top: 6px;
}
.op-more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  color: var(--el-color-primary);
  font-size: 13px;
  outline: none;
  user-select: none;
}
.op-more-icon {
  font-size: 12px;
}
</style>

<style>
/* 操作列按钮不换行（全局：所有表格操作列通用） */
.el-table .op-cell .cell,
.el-table td.el-table__cell.op-cell {
  white-space: nowrap;
}
.op-group {
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.op-group .el-button + .el-button {
  margin-left: 0;
}
.op-group .el-dropdown {
  vertical-align: middle;
}
</style>

<style>
/* 操作列按钮不换行（全局：所有表格操作列通用） */
.el-table .op-cell .cell,
.el-table td.el-table__cell.op-cell {
  white-space: nowrap;
}
.op-group {
  white-space: nowrap;
  display: inline-block;
}
.op-group .el-button + .el-button {
  margin-left: 8px;
}
</style>
