<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">批次追溯（即时库存按批次维度展示，先进先出自动扣减）</span>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="productName" label="产品" min-width="160" />
        <el-table-column label="批次号" width="200">
          <template #default="{ row }">
            <el-tag v-if="row.batchNo" effect="plain">{{ row.batchNo }}</el-tag>
            <el-tag v-else type="info" effect="plain">无批次（未启用批次管理历史库存）</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="qty" label="结存" width="100" align="right" />
        <el-table-column prop="avgCost" label="批次成本" width="110" align="right" />
        <el-table-column prop="amount" label="金额" width="130" align="right" />
      </el-table>
      <el-alert type="info" :closable="false" style="margin-top: 12px"
                title="产品启用批次管理后，入库自动生成批次号；出库按先进先出（FIFO）跨批次自动拆分，流水可按批次追溯。" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { stockApi } from '@/api/stk'

const loading = ref(false)
const rows = ref<any[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const res = await stockApi.quantPage({ pageNo: 1, pageSize: 100 })
    rows.value = (res?.list ?? []).sort((a: any, b: any) => String(a.batchNo).localeCompare(String(b.batchNo)))
  } finally {
    loading.value = false
  }
})
</script>
