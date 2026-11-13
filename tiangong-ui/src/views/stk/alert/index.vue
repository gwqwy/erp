<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">库存预警（低于安全下限 / 高于上限即超储）</span>
        <div style="display:flex;gap:8px">
          <el-button type="warning" :loading="scanning" @click="scanNow">立即扫描并通知</el-button>
          <el-button :icon="Refresh" circle @click="load" />
        </div>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="productName" label="产品" min-width="160" />
        <el-table-column prop="totalQty" label="当前总库存" width="120" align="right" />
        <el-table-column prop="stockMin" label="安全下限" width="110" align="right" />
        <el-table-column prop="stockMax" label="上限" width="110" align="right" />
        <el-table-column label="预警级别" width="140">
          <template #default="{ row }">
            <el-tag :type="row.level === 'low' ? 'danger' : 'warning'">
              {{ row.level === 'low' ? '⚠ 低于安全下限' : '⚠ 超储' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!rows.length && !loading" description="全部正常，无预警 🎉" :image-size="80" />
      <div class="alert-tip">每天 08:00 系统自动扫描，命中低库存会向管理员发送站内消息；也可点击右上角手动扫描。</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { alertScanApi } from '@/api/stock'

const loading = ref(false)
const scanning = ref(false)
const rows = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    const res = await fetch('/admin-api/stk/alert/list', {
      headers: { Authorization: 'Bearer ' + (localStorage.getItem('tiangong_token') || '') }
    }).then(r => r.json())
    rows.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

async function scanNow() {
  scanning.value = true
  try {
    const res: any = await alertScanApi.scanNow()
    if (res.count === 0) ElMessage.success('扫描完成：无低库存商品')
    else if (res.notified) ElMessage.warning('扫描完成：' + res.count + ' 个商品低库存，已发送站内消息')
    else ElMessage.info('扫描完成：' + res.count + ' 个商品低库存')
    await load()
  } finally {
    scanning.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.alert-tip { color: #c0c4cc; font-size: 12px; margin-top: 10px; }
</style>
