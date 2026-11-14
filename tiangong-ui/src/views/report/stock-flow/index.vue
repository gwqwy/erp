<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;gap:8px;align-items:center">
        <span>收发存汇总（期初 = 期末 − 收 + 发）</span>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                        start-placeholder="开始" end-placeholder="结束" style="width:260px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="exportCsv">导出 CSV</el-button>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" border stripe show-summary :summary-method="summary">
      <el-table-column prop="productNo" label="商品编码" width="140" />
      <el-table-column prop="productName" label="商品名称" min-width="180" />
      <el-table-column prop="beginQty" label="期初数量" width="120" align="right" />
      <el-table-column prop="inQty" label="本期入库" width="120" align="right" />
      <el-table-column prop="outQty" label="本期出库" width="120" align="right" />
      <el-table-column prop="endQty" label="期末数量" width="120" align="right" />
      <el-table-column prop="beginAmount" label="期初金额" width="130" align="right" />
      <el-table-column prop="inAmount" label="入库金额" width="130" align="right" />
      <el-table-column prop="outAmount" label="出库金额" width="130" align="right" />
      <el-table-column prop="endAmount" label="期末金额" width="130" align="right" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { stockFlowApi } from '@/api/rpt'

const rows = ref<any[]>([])
const loading = ref(false)
const range = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const params: any = {}
    if (range.value) { params.from = range.value[0]; params.to = range.value[1] }
    else {
      const now = new Date()
      params.from = now.getFullYear() + '-01-01'
      params.to = now.toISOString().slice(0, 10)
    }
    rows.value = await stockFlowApi.summary(params)
  } finally {
    loading.value = false
  }
}

function sum(prop: string) {
  return rows.value.reduce((s, r) => s + Number(r[prop] || 0), 0).toFixed(2)
}

function summary({ columns }: any) {
  return columns.map((col: any, i: number) => {
    if (i === 0) return '合计'
    if (['beginQty', 'inQty', 'outQty', 'endQty', 'beginAmount', 'inAmount', 'outAmount', 'endAmount'].includes(col.property)) {
      return sum(col.property)
    }
    return ''
  })
}

function exportCsv() {
  const header = '商品编码,商品名称,期初数量,本期入库,本期出库,期末数量,期初金额,入库金额,出库金额,期末金额'
  const body = rows.value.map(r =>
    [r.productNo, r.productName, r.beginQty, r.inQty, r.outQty, r.endQty,
      r.beginAmount, r.inAmount, r.outAmount, r.endAmount].join(',')).join('\n')
  const blob = new Blob(['\uFEFF' + header + '\n' + body], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '收发存汇总.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}

onMounted(load)
</script>
