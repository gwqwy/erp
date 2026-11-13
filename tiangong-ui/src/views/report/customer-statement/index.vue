<template>
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
