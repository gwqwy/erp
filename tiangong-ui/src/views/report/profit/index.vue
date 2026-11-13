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

      <div class="table-toolbar"><span style="font-weight: 600">利润简表（{{ period }}）</span></div>
      <el-descriptions v-if="data" :column="3" border size="large" style="max-width: 720px; margin-top: 8px">
        <el-descriptions-item label="营业收入">{{ data.revenue }}</el-descriptions-item>
        <el-descriptions-item label="营业成本">{{ data.cost }}</el-descriptions-item>
        <el-descriptions-item label="利润">
          <span :style="{ color: Number(data.profit) >= 0 ? '#67c23a' : '#f56c6c', fontWeight: 700 }">
            {{ data.profit }}
          </span>
        </el-descriptions-item>
      </el-descriptions>
      <el-alert type="info" :closable="false" style="margin-top: 12px"
                title="口径：收入=6001 贷方-借方；成本=6401 借方-贷方（含红字冲销影响）。M6 将扩展完整三大报表。" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { reportApi } from '@/api/report'

const period = ref('2026-11')
const data = ref<any>(null)

async function load() {
  data.value = await reportApi.profit(period.value)
}

onMounted(load)
</script>
