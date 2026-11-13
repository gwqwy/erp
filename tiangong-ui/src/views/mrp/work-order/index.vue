<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline class="search-form" @submit.prevent>
        <el-form-item label="单号">
          <el-input v-model="searchNo" placeholder="如 MO202611" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <span style="font-weight: 600">生产工单</span>
        <el-button v-perm="'mrp:order:create'" type="primary" :icon="Plus" @click="openCreate">新增</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="no" label="工单号" width="150" />
        <el-table-column prop="productName" label="成品" min-width="150" />
        <el-table-column prop="qty" label="计划数量" width="100" align="right" />
        <el-table-column prop="finishedQty" label="已完工" width="100" align="right" />
        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="percent(row)" :stroke-width="10" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="270">
          <template #default="{ row }">
            <el-button link type="primary" @click="openView(row)">详情</el-button>
            <el-button v-if="row.status === 10" v-perm="'mrp:order:update'" link type="primary"
                       @click="handleDelete(row)">删除</el-button>
            <el-button v-if="row.status === 10" v-perm="'mrp:order:audit'" link type="warning"
                       @click="handleAudit(row)">下达</el-button>
            <el-button v-if="row.status === 10" v-perm="'mrp:order:void'" link type="danger"
                       @click="handleVoid(row)">作废</el-button>
            <el-button v-if="row.status === 30 || row.status === 40" v-perm="'mrp:order:finish'" link type="success"
                       @click="openFinish(row)">完工入库</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNo" :total="total" :page-size="pageSize"
        layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]"
        style="margin-top: 12px; justify-content: flex-end" @change="loadList" />
    </el-card>

    <!-- 新增工单 -->
    <el-dialog v-model="createVisible" title="新增生产工单" width="560px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="成品" required>
          <el-select v-model="createForm.productId" filterable style="width: 100%">
            <el-option v-for="p in products" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="BOM" required>
          <el-select v-model="createForm.bomId" filterable style="width: 100%"
                     placeholder="选择该产品已启用的 BOM">
            <el-option v-for="b in bomOptions" :key="b.value" :label="b.label" :value="b.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="完工仓库" required>
          <el-select v-model="createForm.warehouseId" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.value" :label="w.label" :value="w.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划数量" required>
          <el-input-number v-model="createForm.qty" :min="0.01" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="viewVisible" :title="`生产工单 · ${current?.no || ''}`" size="60%">
      <el-descriptions :column="3" size="small" border style="margin-bottom: 12px">
        <el-descriptions-item label="成品">{{ current?.productName }}</el-descriptions-item>
        <el-descriptions-item label="计划 / 已完工">{{ current?.qty }} / {{ current?.finishedQty }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(current?.status) }}</el-descriptions-item>
      </el-descriptions>
      <div class="items-header">材料需求（按 BOM 展开到原材料）</div>
      <el-table :data="detailItems" border size="small">
        <el-table-column label="材料" min-width="160">
          <template #default="{ row }">{{ productLabel(row.productId) }}</template>
        </el-table-column>
        <el-table-column prop="qty" label="需求" width="100" align="right" />
        <el-table-column prop="issuedQty" label="已领" width="100" align="right" />
        <el-table-column label="领料进度" width="160">
          <template #default="{ row }">
            <el-progress :percentage="issuePercent(row)" :stroke-width="8" />
          </template>
        </el-table-column>
      </el-table>
      <BizChatter biz-type="mrp_work_order" :biz-id="current?.id || ''" />
    </el-drawer>

    <!-- 完工入库 -->
    <el-dialog v-model="finishVisible" title="完工入库" width="440px">
      <el-form label-width="100px">
        <el-form-item label="本次完工数量" required>
          <el-input-number v-model="finishQty" :min="0.01" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入库仓库" required>
          <el-select v-model="finishWarehouse" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.value" :label="w.label" :value="w.value" />
          </el-select>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon
                  title="成品过账 produce_in，成本按工单累计领料成本 ÷ 累计完工数量估算" />
      </el-form>
      <template #footer>
        <el-button @click="finishVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFinish">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import BizChatter from '@/components/BizChatter.vue'
import { workOrderApi, bomApi } from '@/api/mrp'
import { productApi, warehouseApi } from '@/api/bas'

const STATUS = [
  { value: 10, label: '计划', tag: 'info' },
  { value: 30, label: '已下达', tag: 'primary' },
  { value: 40, label: '生产中', tag: 'warning' },
  { value: 50, label: '已完成', tag: 'success' },
  { value: 60, label: '已作废', tag: 'danger' }
]

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const searchNo = ref('')

const createVisible = ref(false)
const createForm = ref({ productId: '', bomId: '', warehouseId: '', qty: 1, remark: '' })
const viewVisible = ref(false)
const current = ref<any>(null)
const detailItems = ref<any[]>([])
const finishVisible = ref(false)
const finishQty = ref(1)
const finishWarehouse = ref('')
const finishId = ref('')

const products = ref<Array<{ label: string; value: string }>>([])
const productRaw = ref<any[]>([])
const bomOptions = ref<Array<{ label: string; value: string }>>([])
const warehouses = ref<Array<{ label: string; value: string }>>([])

function statusLabel(v: number) {
  return STATUS.find(s => s.value === v)?.label ?? v
}
function statusTag(v: number) {
  return (STATUS.find(s => s.value === v)?.tag || 'info') as any
}
function percent(row: any) {
  return Math.min(100, Math.round((Number(row.finishedQty || 0) / Number(row.qty || 1)) * 100))
}
function issuePercent(row: any) {
  return Math.min(100, Math.round((Number(row.issuedQty || 0) / Number(row.qty || 1)) * 100))
}
function productLabel(id: string) {
  const hit = productRaw.value.find(p => String(p.id) === String(id))
  return hit ? `${hit.name}${hit.spec ? ' / ' + hit.spec : ''}` : id
}

async function loadList() {
  loading.value = true
  try {
    const res = await workOrderApi.page({ pageNo: pageNo.value, pageSize: pageSize.value, no: searchNo.value || undefined })
    list.value = res?.list ?? []
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

function reset() {
  searchNo.value = ''
  pageNo.value = 1
  loadList()
}

async function openCreate() {
  createForm.value = { productId: '', bomId: '', warehouseId: warehouses.value[0]?.value || '', qty: 1, remark: '' }
  createVisible.value = true
}

async function saveCreate() {
  if (!createForm.value.productId || !createForm.value.bomId || !createForm.value.warehouseId) {
    ElMessage.warning('请完整选择成品/BOM/仓库')
    return
  }
  saving.value = true
  try {
    await workOrderApi.create({ ...createForm.value, qty: Number(createForm.value.qty) })
    ElMessage.success('创建成功')
    createVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function openView(row: any) {
  const detail = await workOrderApi.detail(row.id)
  current.value = { ...detail.order, productName: productLabel(detail.order.productId) }
  detailItems.value = detail.items
  viewVisible.value = true
}

async function handleAudit(row: any) {
  await ElMessageBox.confirm('下达后按 BOM 展开生成材料需求行，确认下达吗？', '提示', { type: 'warning' })
  await workOrderApi.audit(row.id)
  ElMessage.success('已下达')
  loadList()
}

async function handleVoid(row: any) {
  await ElMessageBox.confirm('确认作废该工单吗？', '提示', { type: 'warning' })
  await workOrderApi.voidOrder(row.id)
  ElMessage.success('已作废')
  loadList()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除该计划草稿吗？', '提示', { type: 'warning' })
  await workOrderApi.delete(row.id)
  ElMessage.success('删除成功')
  loadList()
}

function openFinish(row: any) {
  finishId.value = row.id
  finishQty.value = Math.max(0.01, Number(row.qty) - Number(row.finishedQty || 0))
  finishWarehouse.value = row.warehouseId ? String(row.warehouseId) : warehouses.value[0]?.value || ''
  finishVisible.value = true
}

async function saveFinish() {
  saving.value = true
  try {
    const cost = await workOrderApi.finish({
      id: finishId.value, qty: Number(finishQty.value), warehouseId: finishWarehouse.value
    })
    ElMessage.success(`完工入库成功，估算单位成本 ${cost}`)
    finishVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loadList()
  const pp = await productApi.page({ pageNo: 1, pageSize: 200, status: 0 })
  productRaw.value = pp?.list ?? []
  products.value = productRaw.value.map((p: any) => ({
    label: `${p.name}${p.spec ? ' / ' + p.spec : ''}`,
    value: p.id
  }))
  warehouses.value = (await warehouseApi.list({ status: 0 })).map((w: any) => ({ label: w.name, value: w.id }))
  const boms = await bomApi.page({ pageNo: 1, pageSize: 100, status: 1 })
  bomOptions.value = (boms?.list ?? []).map((b: any) => ({
    label: `${productLabel(b.productId)} v${b.version}`,
    value: b.id
  }))
})
</script>

<style scoped>
.items-header {
  font-weight: 600;
  margin: 12px 0 8px;
}
</style>
