<template>
  <div class="screen" :class="{ dark: darkMode }">
    <div class="screen-header">
      <span class="screen-title">天工 ERP · 经营大屏</span>
      <div class="screen-actions no-print">
        <el-button size="small" @click="toggleDark">{{ darkMode ? '浅色' : '深色' }}</el-button>
        <el-button size="small" @click="exit">退出大屏</el-button>
      </div>
    </div>

    <el-row :gutter="20" class="screen-cards">
      <el-col :span="4" v-for="card in cards" :key="card.label">
        <div class="screen-card">
          <div class="screen-card-label">{{ card.label }}</div>
          <div class="screen-card-value">{{ card.value }}</div>
          <div class="screen-card-suffix">{{ card.suffix }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="screen-charts">
      <el-col :span="16">
        <div class="screen-panel">
          <div class="screen-panel-title">销售 / 采购月度趋势</div>
          <div ref="trendRef" class="screen-trend" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="screen-panel">
          <div class="screen-panel-title">待办与预警</div>
          <div class="screen-todo">
            <div v-for="t in todos" :key="t.label" class="screen-todo-item">
              <span>{{ t.label }}</span>
              <span class="screen-todo-value" :class="{ hot: t.value > 0 }">{{ t.value }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api/rpt'
import { bpmApi } from '@/api/bpm'
import { messageApi } from '@/api/system'
import { alertScanApi } from '@/api/stock'

const darkMode = ref(true)
const summary = ref<any>(null)
const trendRef = ref<HTMLDivElement>()
const todoCount = ref(0)
const unreadCount = ref(0)
const lowCount = ref(0)
let chart: echarts.ECharts | null = null
let timer: any = null

const cards = computed(() => [
  { label: '本月销售额', value: fmt(latestAmount(summary.value?.salesTrend)), suffix: '元' },
  { label: '本月采购额', value: fmt(latestAmount(summary.value?.purchaseTrend)), suffix: '元' },
  { label: '库存总金额', value: fmt(summary.value?.stockValue), suffix: '元' },
  { label: '在产工单', value: summary.value?.activeWorkOrders ?? 0, suffix: '张' },
  { label: '未核销应收', value: fmt(summary.value?.unsettledReceivable), suffix: '元' },
  { label: '未核销应付', value: fmt(summary.value?.unsettledPayable), suffix: '元' }
])

const todos = computed(() => [
  { label: '待办审批任务', value: todoCount.value },
  { label: '未读站内消息', value: unreadCount.value },
  { label: '低库存预警商品', value: lowCount.value }
])

function fmt(v: any) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function latestAmount(trend: any) {
  if (!Array.isArray(trend) || !trend.length) return 0
  return trend[trend.length - 1]?.amt ?? 0
}

async function load() {
  summary.value = await dashboardApi.summary()
  try {
    const tasks = await bpmApi.myTasks()
    todoCount.value = Array.isArray(tasks) ? tasks.length : 0
  } catch { }
  try {
    unreadCount.value = Number(await messageApi.myUnreadCount()) || 0
  } catch { }
  try {
    lowCount.value = ((await alertScanApi.lowList()) || []).length
  } catch { }
  renderTrend()
}

function renderTrend() {
  if (!trendRef.value) return
  if (!chart) chart = echarts.init(trendRef.value)
  const sales = summary.value?.salesTrend || []
  const pur = summary.value?.purchaseTrend || []
  const textColor = darkMode.value ? '#c8c9cc' : '#333'
  chart.setOption({
    textStyle: { color: textColor },
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '采购额'], textStyle: { color: textColor } },
    grid: { left: 60, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: sales.map((s: any) => s.ym), axisLabel: { color: textColor } },
    yAxis: { type: 'value', axisLabel: { color: textColor }, splitLine: { lineStyle: { opacity: 0.2 } } },
    series: [
      { name: '销售额', type: 'bar', data: sales.map((s: any) => s.amount), itemStyle: { color: '#409eff' } },
      { name: '采购额', type: 'bar', data: pur.map((s: any) => s.amount), itemStyle: { color: '#67c23a' } }
    ]
  })
}

function toggleDark() {
  darkMode.value = !darkMode.value
  setTimeout(renderTrend, 100)
}

function enterFullscreen() {
  const el = document.documentElement
  if (el.requestFullscreen) el.requestFullscreen().catch(() => { })
}

function exit() {
  if (document.fullscreenElement) document.exitFullscreen()
  history.back()
}

onMounted(async () => {
  await load()
  enterFullscreen()
  timer = setInterval(load, 30000)
  window.addEventListener('resize', renderTrend)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', renderTrend)
})
</script>

<style scoped>
.screen { padding: 20px; min-height: 100vh; background: #f0f2f5; color: #333; transition: all .3s; }
.screen.dark { background: #0d1117; color: #c8c9cc; }
.screen-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.screen-title { font-size: 26px; font-weight: 700; letter-spacing: 4px; }
.screen-card { border: 1px solid rgba(144, 147, 153, .25); border-radius: 8px; padding: 16px; text-align: center; }
.dark .screen-card { background: rgba(255, 255, 255, .04); }
.screen-card-label { font-size: 14px; opacity: .75; }
.screen-card-value { font-size: 30px; font-weight: 700; margin: 6px 0; font-variant-numeric: tabular-nums; }
.screen-card-suffix { font-size: 12px; opacity: .6; }
.screen-panel { border: 1px solid rgba(144, 147, 153, .25); border-radius: 8px; padding: 16px; height: 100%; }
.dark .screen-panel { background: rgba(255, 255, 255, .04); }
.screen-panel-title { font-size: 16px; font-weight: 600; margin-bottom: 10px; }
.screen-trend { height: 320px; }
.screen-todo-item { display: flex; justify-content: space-between; align-items: center; padding: 18px 8px; border-bottom: 1px dashed rgba(144, 147, 153, .3); font-size: 16px; }
.screen-todo-value { font-size: 30px; font-weight: 700; font-variant-numeric: tabular-nums; }
.screen-todo-value.hot { color: #e6a23c; }
.dark .screen-todo-value.hot { color: #f56c6c; }
</style>
