<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline class="search-form" @submit.prevent>
        <el-form-item label="计划号">
          <el-input v-model="searchNo" placeholder="如 PL202611" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <span style="font-weight: 600">MRP 计划（试算结果落库）</span>
      </div>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="no" label="计划号" width="150" />
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : 'info'">
              {{ row.status === 2 ? '已转采购' : '已保存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="净需求摘要" min-width="220">
          <template #default="{ row }">{{ summaryOf(row.resultJson) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">结果</el-button>
            <el-button v-if="row.status === 1" v-perm="'mrp:plan:convert'" link type="warning"
                       @click="openConvert(row)">转采购</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNo" :total="total" :page-size="pageSize"
        layout="total, prev, pager, next" style="margin-top: 12px; justify-content: flex-end"
        @current-change="loadList" />
    </el-card>

    <!-- 结果明细 -->
    <el-drawer v-model="detailVisible" :title="`计划结果 · ${current?.no || ''}`" size="560px">
      <el-table :data="detailLines" border size="small">
        <el-table-column prop="productName" label="原材料" min-width="150" />
        <el-table-column prop="stockQty" label="现有库存" width="100" align="right" />
        <el-table-column prop="netQty" label="净需求" width="100" align="right" />
      </el-table>
    </el-drawer>

    <!-- 转采购 -->
    <el-dialog v-model="convertVisible" title="计划转采购订单" width="440px">
      <el-form label-width="90px">
        <el-form-item label="供应商" required>
          <el-select v-model="convertSupplier" filterable style="width: 100%">
            <el-option v-for="o in supplierOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon
                  title="将按计划中净需求 > 0 的行生成采购订单草稿（价格为 0，待询价后修改）" />
      </el-form>
      <template #footer>
        <el-button @click="convertVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveConvert">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { mrpPlanApi } from '@/api/mrp'
import { partnerApi } from '@/api/bas'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const searchNo = ref('')

const detailVisible = ref(false)
const current = ref<any>(null)
const detailLines = ref<any[]>([])

const convertVisible = ref(false)
const convertId = ref('')
const convertSupplier = ref('')
const supplierOptions = ref<Array<{ label: string; value: string }>>([])

function summaryOf(json: string) {
  try {
    const lines = JSON.parse(json || '[]')
    return lines.map((l: any) => `${l.productName}×${Number(l.netQty)}`).join('，') || '—'
  } catch {
    return '—'
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await mrpPlanApi.page({ pageNo: pageNo.value, pageSize: pageSize.value, no: searchNo.value || undefined })
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

function openDetail(row: any) {
  current.value = row
  detailLines.value = JSON.parse(row.resultJson || '[]')
  detailVisible.value = true
}

async function openConvert(row: any) {
  convertId.value = row.id
  if (!supplierOptions.value.length) {
    supplierOptions.value = (await partnerApi.page({ pageNo: 1, pageSize: 200, isSupplier: true, status: 0 }))
      .list.map((p: any) => ({ label: p.name, value: p.id }))
  }
  convertSupplier.value = ''
  convertVisible.value = true
}

async function saveConvert() {
  if (!convertSupplier.value) {
    ElMessage.warning('请选择供应商')
    return
  }
  saving.value = true
  try {
    const orderId = await mrpPlanApi.toPurchaseOrder({ id: convertId.value, supplierId: convertSupplier.value })
    ElMessage.success(`已生成采购订单草稿（id ${orderId}），请到采购订单中审核`)
    convertVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

onMounted(loadList)
</script>
