<template>
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
