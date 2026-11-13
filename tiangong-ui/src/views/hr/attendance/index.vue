<!-- 打卡 + 考勤记录（紧凑工具页） -->
<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="员工">
          <el-select v-model="employeeId" filterable placeholder="选择员工打卡" style="width: 220px">
            <el-option v-for="o in employees" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="success" :disabled="!employeeId" @click="clock('in')">上班打卡</el-button>
          <el-button type="primary" :disabled="!employeeId" @click="clock('out')">下班打卡</el-button>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button :icon="Search" @click="loadList">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column label="员工" width="130">
          <template #default="{ row }">{{ employeeLabel(row.employeeId) }}</template>
        </el-table-column>
        <el-table-column prop="attDate" label="日期" width="120" />
        <el-table-column prop="checkIn" label="上班" width="100" />
        <el-table-column prop="checkOut" label="下班" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" />
      </el-table>
      <el-pagination v-model:current-page="pageNo" :total="total" :page-size="20"
                     layout="total, prev, pager, next" style="margin-top: 12px; justify-content: flex-end"
                     @current-change="loadList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { attendanceApi } from '@/api/hr'
import { employeeApi } from '@/api/hr'

const STATUS = [
  { value: 0, label: '正常', tag: 'success' },
  { value: 1, label: '迟到', tag: 'warning' },
  { value: 2, label: '早退', tag: 'warning' },
  { value: 3, label: '缺勤', tag: 'danger' },
  { value: 4, label: '事假', tag: 'info' },
  { value: 5, label: '年假', tag: 'info' }
]

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const employeeId = ref('')
const range = ref<[string, string] | null>(null)
const employees = ref<Array<{ label: string; value: string }>>([])

function statusLabel(v: number) {
  return STATUS.find(s => s.value === v)?.label ?? v
}
function statusTag(v: number) {
  return (STATUS.find(s => s.value === v)?.tag || 'info') as any
}
function employeeLabel(id: string) {
  return employees.value.find(o => String(o.value) === String(id))?.label ?? id
}

async function loadList() {
  loading.value = true
  try {
    const res = await attendanceApi.page({
      pageNo: pageNo.value, pageSize: 20, employeeId: employeeId.value || undefined,
      begin: range.value?.[0], end: range.value?.[1]
    })
    list.value = res?.list ?? []
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

async function clock(dir: 'in' | 'out') {
  try {
    if (dir === 'in') await attendanceApi.checkIn(employeeId.value)
    else await attendanceApi.checkOut(employeeId.value)
    ElMessage.success(dir === 'in' ? '上班打卡成功' : '下班打卡成功')
    loadList()
  } catch { /* 错误已 toast */ }
}

onMounted(async () => {
  loadList()
  employees.value = (await employeeApi.page({ pageNo: 1, pageSize: 100 }))
    .list.map((e: any) => ({ label: e.name, value: e.id }))
})
</script>
