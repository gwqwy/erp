<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="结果">
          <el-select v-model="success" clearable placeholder="全部" style="width: 120px">
            <el-option label="成功" :value="true" />
            <el-option label="失败" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar"><span style="font-weight: 600">操作日志（自动记录全部写操作）</span></div>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="url" label="接口" min-width="260" />
        <el-table-column prop="httpMethod" label="方式" width="90">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.httpMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paramsText" label="参数" min-width="220" show-overflow-tooltip />
        <el-table-column prop="costMs" label="耗时(ms)" width="100" align="right" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pageNo" :total="total" :page-size="20"
                     layout="total, prev, pager, next" style="margin-top: 12px; justify-content: flex-end"
                     @current-change="load" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { operLogApi } from '@/api/more'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const success = ref<boolean | undefined>(undefined)

async function load() {
  loading.value = true
  try {
    const res = await operLogApi.page({ pageNo: pageNo.value, pageSize: 20, success: success.value })
    list.value = res?.list ?? []
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
