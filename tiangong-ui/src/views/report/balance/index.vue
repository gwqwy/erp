<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="会计期间">
          <el-date-picker v-model="period" type="month" value-format="YYYY-MM" :clearable="false" style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar"><span style="font-weight: 600">科目余额表（{{ period }}，已过账凭证汇总）</span></div>
      <el-table :data="lines" v-loading="loading" border stripe show-summary :summary-method="sum">
        <el-table-column prop="subject" label="科目" min-width="200" />
        <el-table-column prop="debit" label="借方发生" width="140" align="right" />
        <el-table-column prop="credit" label="贷方发生" width="140" align="right" />
        <el-table-column prop="balance" label="借贷差额" width="140" align="right" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { reportApi } from '@/api/report'

const loading = ref(false)
const period = ref('2026-11')
const lines = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    lines.value = (await reportApi.balance(period.value)) ?? []
  } finally {
    loading.value = false
  }
}

function sum({ columns }: { columns: any[] }) {
  return columns.map((col, i) => {
    if (i === 0) return '合计'
    if (col.property === 'debit') return lines.value.reduce((s, r) => s + Number(r.debit || 0), 0).toFixed(2)
    if (col.property === 'credit') return lines.value.reduce((s, r) => s + Number(r.credit || 0), 0).toFixed(2)
    return ''
  })
}

onMounted(load)
</script>
