<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">应收账龄（未核销客户汇总，按最早到期日计算逾期天数）</span>
        <el-button :icon="Refresh" circle @click="load" />
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="partnerName" label="客户" min-width="160" />
        <el-table-column prop="unreceivedAmount" label="未核销金额" width="140" align="right" />
        <el-table-column prop="earliestDue" label="最早到期日" width="130" />
        <el-table-column label="逾期" width="140">
          <template #default="{ row }">
            <el-tag v-if="Number(row.overdueDays) > 0" type="danger">逾期 {{ row.overdueDays }} 天</el-tag>
            <el-tag v-else type="success">未逾期</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { expenseApi } from '@/api/more'

const loading = ref(false)
const rows = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    rows.value = (await expenseApi.aging()) ?? []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
