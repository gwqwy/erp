<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="月份">
          <el-date-picker v-model="period" type="month" value-format="YYYY-MM" :clearable="false" style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">统计</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar"><span style="font-weight: 600">月度考勤汇总（{{ period }}）</span></div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="employeeName" label="员工" min-width="130" />
        <el-table-column prop="presentDays" label="出勤" width="90" align="center" />
        <el-table-column prop="lateDays" label="迟到" width="90" align="center" />
        <el-table-column prop="earlyDays" label="早退" width="90" align="center" />
        <el-table-column prop="absentDays" label="缺勤" width="90" align="center" />
        <el-table-column prop="personalLeaveDays" label="事假" width="90" align="center" />
        <el-table-column prop="annualLeaveDays" label="年假" width="90" align="center" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { attendanceApi } from '@/api/hr'

const loading = ref(false)
const period = ref('2026-11')
const rows = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    rows.value = (await attendanceApi.summary(period.value)) ?? []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
