<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline class="search-form" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="status" clearable placeholder="全部" style="width: 130px">
            <el-option label="未核销" :value="1" />
            <el-option label="部分核销" :value="2" />
            <el-option label="已结清" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar"><span style="font-weight: 600">应收台账（销售出库自动生成）</span></div>
      <el-table :data="list" v-loading="loading" border stripe show-summary :summary-method="sumSummary">
        <el-table-column prop="bizNo" label="来源单号" width="160" />
        <el-table-column prop="partnerName" label="客户" min-width="160" />
        <el-table-column prop="amount" label="应收金额" width="120" align="right" />
        <el-table-column prop="receivedAmount" label="已核销" width="120" align="right" />
        <el-table-column prop="unreceivedAmount" label="未核销" width="120" align="right" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 3 ? 'success' : row.status === 2 ? 'warning' : 'info'">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNo" :total="total" :page-size="20"
        layout="total, prev, pager, next" style="margin-top: 12px; justify-content: flex-end"
        @current-change="loadList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { receivableApi } from '@/api/fin'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const status = ref<number | undefined>(undefined)

function statusLabel(v: number) {
  return { 1: '未核销', 2: '部分核销', 3: '已结清', 4: '已作废' }[v] ?? v
}

async function loadList() {
  loading.value = true
  try {
    const res = await receivableApi.page({ pageNo: pageNo.value, pageSize: 20, status: status.value })
    list.value = res?.list ?? []
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

function reset() {
  status.value = undefined
  pageNo.value = 1
  loadList()
}

function sumSummary({ columns }: { columns: any[] }) {
  return columns.map((col, i) => {
    if (i === 0) return '合计'
    if (col.property === 'amount') return list.value.reduce((s, r) => s + Number(r.amount || 0), 0).toFixed(2)
    if (col.property === 'receivedAmount') return list.value.reduce((s, r) => s + Number(r.receivedAmount || 0), 0).toFixed(2)
    if (col.property === 'unreceivedAmount') return list.value.reduce((s, r) => s + Number(r.unreceivedAmount || 0), 0).toFixed(2)
    return ''
  })
}

onMounted(loadList)
</script>
