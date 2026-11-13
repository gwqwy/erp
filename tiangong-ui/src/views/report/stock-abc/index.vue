<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>库存 ABC 分析（按库存占用金额：累计 80% 为 A，80~95% 为 B，其余 C）</span>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" border stripe :row-class-name="rowClass">
      <el-table-column prop="productName" label="商品" min-width="200" />
      <el-table-column prop="qty" label="库存数量" width="130" align="right" />
      <el-table-column prop="amount" label="占用金额" width="150" align="right" />
      <el-table-column label="金额占比" width="110" align="right">
        <template #default="{ row }">{{ (Number(row.amountRatio) * 100).toFixed(2) }}%</template>
      </el-table-column>
      <el-table-column label="累计占比" width="110" align="right">
        <template #default="{ row }">{{ (Number(row.cumulativeRatio) * 100).toFixed(2) }}%</template>
      </el-table-column>
      <el-table-column label="分类" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.abcClass === 'A' ? 'danger' : row.abcClass === 'B' ? 'warning' : 'info'">
            {{ row.abcClass }} 类
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { analysisApi } from '@/api/rpt'

const rows = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await analysisApi.stockAbc()
  } finally {
    loading.value = false
  }
}

function rowClass({ row }: any) {
  return row.abcClass === 'A' ? 'abc-a' : ''
}

onMounted(load)
</script>

<style scoped>
:deep(.abc-a) { background: #fef0f0; }
</style>
