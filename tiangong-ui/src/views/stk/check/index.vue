<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">库存盘点</span>
        <el-form inline @submit.prevent style="margin-bottom: 0">
          <el-form-item label="仓库" style="margin-bottom: 0">
            <el-select v-model="warehouseId" filterable style="width: 180px">
              <el-option v-for="w in warehouses" :key="w.value" :label="w.label" :value="w.value" />
            </el-select>
          </el-form-item>
          <el-button v-perm="'stk:check:create'" type="primary" @click="createCheck">按账面快照建盘点单</el-button>
        </el-form>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="no" label="盘点单号" width="150" />
        <el-table-column prop="checkDate" label="盘点日期" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 10 ? 'info' : row.status === 30 ? 'success' : 'danger'">
              {{ row.status === 10 ? '草稿' : row.status === 30 ? '已审核' : row.status === 50 ? '已作废' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">录入实盘</el-button>
            <el-button v-if="row.status === 10" v-perm="'stk:check:audit'" link type="warning"
                       @click="audit(row)">审核</el-button>
            <el-button v-if="row.status === 10" v-perm="'stk:check:delete'" link type="danger"
                       @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-drawer v-model="detailVisible" :title="`实盘录入 · ${current?.no || ''}`" size="620px">
        <el-table :data="detailItems" border size="small">
          <el-table-column label="产品" min-width="140">
            <template #default="{ row }">{{ productLabel(row.productId) }}</template>
          </el-table-column>
          <el-table-column prop="bookQty" label="账面" width="100" align="right" />
          <el-table-column label="实盘" width="140">
            <template #default="{ row }">
              <el-input-number v-if="editable" v-model="row.realQty" :min="0" :precision="2"
                               :controls="false" style="width: 100%" size="small" />
              <span v-else>{{ row.realQty }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="editable" style="margin-top: 12px; text-align: right">
          <el-button type="primary" size="small" @click="saveRealQty">保存实盘</el-button>
        </div>
      </el-drawer>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { checkApi } from '@/api/stock'
import { productApi, warehouseApi } from '@/api/bas'

const loading = ref(false)
const list = ref<any[]>([])
const warehouseId = ref('')
const warehouses = ref<Array<{ label: string; value: string }>>([])
const productRaw = ref<any[]>([])
const detailVisible = ref(false)
const current = ref<any>(null)
const detailItems = ref<any[]>([])
const editable = ref(false)

function statusTag(row: any) { return '' }
function productLabel(id: string) {
  const hit = productRaw.value.find(p => String(p.id) === String(id))
  return hit ? hit.name : id
}

async function load() {
  loading.value = true
  try {
    list.value = (await checkApi.page({ pageNo: 1, pageSize: 50 })).list ?? []
  } finally {
    loading.value = false
  }
}

async function createCheck() {
  if (!warehouseId.value) {
    ElMessage.warning('请选择仓库')
    return
  }
  await ElMessageBox.confirm('按当前账面库存生成盘点快照？', '提示', { type: 'warning' })
  await checkApi.create({ warehouseId: warehouseId.value, remark: '页面创建' })
  ElMessage.success('盘点单已创建')
  load()
}

async function openDetail(row: any) {
  const detail = await checkApi.detail(row.id)
  current.value = detail.check
  detailItems.value = detail.items
  editable.value = row.status === 10
  detailVisible.value = true
}

async function saveRealQty() {
  for (const row of detailItems.value) {
    await checkApi.updateRealQty(row.id, Number(row.realQty))
  }
  ElMessage.success('实盘已保存')
  detailVisible.value = false
  load()
}

async function audit(row: any) {
  await ElMessageBox.confirm('审核后按盈亏自动过账并生成凭证，确认？', '提示', { type: 'warning' })
  await checkApi.audit(row.id)
  ElMessage.success('盘点已审核')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该盘点草稿？', '提示', { type: 'warning' })
  await checkApi.delete(row.id)
  ElMessage.success('删除成功')
  load()
}

import { ElMessageBox } from 'element-plus'

onMounted(async () => {
  load()
  warehouses.value = (await warehouseApi.list({ status: 0 })).map((w: any) => ({ label: w.name, value: w.id }))
  const pp = await productApi.page({ pageNo: 1, pageSize: 200, status: 0 })
  productRaw.value = pp?.list ?? []
})
</script>
