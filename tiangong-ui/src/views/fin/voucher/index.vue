<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline class="search-form" @submit.prevent>
        <el-form-item label="凭证字">
          <el-input v-model="no" placeholder="如 PZ202611" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar"><span style="font-weight: 600">总账凭证（业务审核自动生成）</span></div>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="no" label="凭证字" width="150" />
        <el-table-column prop="voucherDate" label="凭证日期" width="120" />
        <el-table-column prop="period" label="期间" width="90" />
        <el-table-column prop="bizType" label="来源业务" width="120">
          <template #default="{ row }">
            <el-tag effect="plain">{{ bizLabel(row.bizType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceNo" label="来源单号" width="150" />
        <el-table-column prop="totalDebit" label="借方合计" width="120" align="right" />
        <el-table-column prop="totalCredit" label="贷方合计" width="120" align="right" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNo" :total="total" :page-size="20"
        layout="total, prev, pager, next" style="margin-top: 12px; justify-content: flex-end"
        @current-change="loadList" />

      <el-dialog v-model="detailVisible" :title="`凭证明细 · ${current?.no || ''}`" width="720px">
        <el-descriptions :column="3" size="small" border style="margin-bottom: 12px">
          <el-descriptions-item label="来源业务">{{ bizLabel(current?.bizType) }}</el-descriptions-item>
          <el-descriptions-item label="来源单号">{{ current?.sourceNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="凭证日期">{{ current?.voucherDate }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detailItems" border size="small" show-summary :summary-method="sumSummary">
          <el-table-column prop="subjectNo" label="科目" width="90" />
          <el-table-column prop="summary" label="摘要" min-width="200" />
          <el-table-column prop="debit" label="借方" width="120" align="right" />
          <el-table-column prop="credit" label="贷方" width="120" align="right" />
        </el-table>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { voucherApi } from '@/api/fin'

const BIZ_LABELS: Record<string, string> = {
  sale_out: '销售出库',
  purchase_in: '采购入库',
  receipt: '收款',
  payment: '付款'
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const no = ref('')
const detailVisible = ref(false)
const current = ref<any>(null)
const detailItems = ref<any[]>([])

function bizLabel(t: string) {
  return BIZ_LABELS[t] ?? t ?? '手工'
}

async function loadList() {
  loading.value = true
  try {
    const res = await voucherApi.page({ pageNo: pageNo.value, pageSize: 20, no: no.value || undefined })
    list.value = res?.list ?? []
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

function reset() {
  no.value = ''
  pageNo.value = 1
  loadList()
}

async function openDetail(row: any) {
  const detail = await voucherApi.detail(row.id)
  current.value = detail.voucher
  detailItems.value = detail.items
  detailVisible.value = true
}

function sumSummary({ columns }: { columns: any[] }) {
  return columns.map((col, i) => {
    if (i === 0) return '合计'
    if (col.property === 'debit') return detailItems.value.reduce((s, r) => s + Number(r.debit || 0), 0).toFixed(2)
    if (col.property === 'credit') return detailItems.value.reduce((s, r) => s + Number(r.credit || 0), 0).toFixed(2)
    return ''
  })
}

onMounted(loadList)
</script>
