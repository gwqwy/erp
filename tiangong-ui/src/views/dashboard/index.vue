<template>
  <div class="page-card">
    <!-- 指标卡片 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-suffix">{{ card.suffix }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="table-toolbar" style="margin-top:16px">
      <span></span>
      <el-button type="warning" @click="router.push('/screen')">进入经营大屏</el-button>
    </div>

    <!-- 待办提醒 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="8" v-for="card in todoCards" :key="card.label">
        <el-card shadow="hover" class="todo-card" @click="card.go">
          <div class="todo-row">
            <span class="todo-label">{{ card.label }}</span>
            <el-badge :value="card.value" :type="card.value > 0 ? 'danger' : 'info'" :max="99" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <span>销售 / 采购月度趋势（已审核单据，近 6 个月）</span>
      </template>
      <div ref="chartRef" class="trend-chart" />
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header><span>系统状态</span></template>
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          当前已交付：进销存闭环 · 业务财务自动过账 · 生产制造 · CRM · 驾驶舱。
          待办：低代码平台、移动端 H5、钉钉/企微集成（M5）。
        </template>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api/rpt'
import { bpmApi } from '@/api/bpm'
import { messageApi } from '@/api/system'
import { alertScanApi } from '@/api/stock'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMenuStore } from '@/stores/menu'

const userStore = useUserStore()
const menuStore = useMenuStore()

const router = useRouter()
const summary = ref<any>(null)
const todoCards = ref<Array<{ label: string; value: number; go: () => void }>>([
  { label: '待办审批任务', value: 0, go: () => router.push('/bpm/my-tasks') },
  { label: '未读站内消息', value: 0, go: () => router.push('/system/message') },
  { label: '低库存预警商品', value: 0, go: () => router.push('/stk/alert') }
])

async function loadTodos() {
  try {
    const tasks = await bpmApi.myTasks()
    todoCards.value[0].value = Array.isArray(tasks) ? tasks.length : Number(tasks?.total ?? 0) || 0
  } catch { /* 审批接口失败不阻塞 */ }
  try {
    todoCards.value[1].value = Number(await messageApi.myUnreadCount()) || 0
  } catch { /* 消息接口失败不阻塞 */ }
  try {
    const lows = await alertScanApi.lowList()
    todoCards.value[2].value = (lows || []).length
  } catch { /* 预警接口失败不阻塞 */ }
}
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const cards = computed(() => [
  { label: '本月销售额', value: monthAmount(summary.value?.salesTrend), suffix: '元' },
  { label: '本月采购额', value: monthAmount(summary.value?.purchaseTrend), suffix: '元' },
  { label: '库存总金额', value: fmt(summary.value?.stockValue), suffix: '元' },
  { label: '在产工单', value: summary.value?.activeWorkOrders ?? 0, suffix: '张' },
  { label: '未核销应收', value: fmt(summary.value?.unsettledReceivable), suffix: '元' },
  { label: '未核销应付', value: fmt(summary.value?.unsettledPayable), suffix: '元' }
])

function fmt(v: any) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function monthAmount(trend: any[] | undefined) {
  if (!trend?.length) return '0'
  return fmt(trend[trend.length - 1]?.amt)
}

function renderChart() {
  if (!chartRef.value || !summary.value) return
  chart = chart || echarts.init(chartRef.value)
  const sales = summary.value.salesTrend ?? []
  const purchase = summary.value.purchaseTrend ?? []
  const months = [...new Set([...sales.map((s: any) => s.ym), ...purchase.map((p: any) => p.ym)])].sort()
  const pick = (arr: any[], m: string) => Number(arr.find((x: any) => x.ym === m)?.amt ?? 0)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '采购额'] },
    grid: { left: 60, right: 30, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value' },
    series: [
      { name: '销售额', type: 'bar', data: months.map(m => pick(sales, m)), itemStyle: { color: '#409eff' } },
      { name: '采购额', type: 'bar', data: months.map(m => pick(purchase, m)), itemStyle: { color: '#67c23a' } }
    ]
  })
}

onMounted(async () => {
  loadTodos()
  summary.value = await dashboardApi.summary(6)
  renderChart()
  window.addEventListener('resize', () => chart?.resize())
})

const countMenus = computed(() => {
  let count = 0
  for (const dir of menuStore.menus) {
    count += (dir.children || []).filter(c => c.type === 2).length
  }
  return count
})
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-size: 22px; font-weight: 700; color: #303133; margin: 6px 0 2px; }
.stat-suffix { color: #c0c4cc; font-size: 12px; }
.trend-chart { height: 320px; }

.todo-card { cursor: pointer; }
.todo-row { display: flex; justify-content: space-between; align-items: center; }
.todo-label { font-size: 14px; color: #606266; }
</style>
