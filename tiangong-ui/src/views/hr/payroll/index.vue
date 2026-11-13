<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="期间">
          <el-date-picker v-model="period" type="month" value-format="YYYY-MM" :clearable="false" style="width: 130px" />
        </el-form-item>
        <el-form-item label="员工">
          <el-select v-model="employeeId" filterable style="width: 180px">
            <el-option v-for="o in employees" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="calculate">试算</el-button>
          <el-button type="success" :disabled="!breakdown" v-perm="'hr:payroll:generate'" @click="generate">
            生成薪资记录
          </el-button>
        </el-form-item>
      </el-form>

      <el-descriptions v-if="breakdown" :column="3" border size="large" style="max-width: 860px; margin-top: 8px">
        <el-descriptions-item label="员工">{{ breakdown.employeeName }}</el-descriptions-item>
        <el-descriptions-item label="基本工资">{{ breakdown.baseSalary }}</el-descriptions-item>
        <el-descriptions-item label="加班时数">{{ breakdown.overtimeHours }} h</el-descriptions-item>
        <el-descriptions-item label="加班费(1.5x)">{{ breakdown.overtimeAmount }}</el-descriptions-item>
        <el-descriptions-item label="缺勤/事假">{{ breakdown.absenceDays }} 天</el-descriptions-item>
        <el-descriptions-item label="缺勤扣款">{{ breakdown.absenceDeduction }}</el-descriptions-item>
        <el-descriptions-item label="奖励">{{ breakdown.rewardAmount }}</el-descriptions-item>
        <el-descriptions-item label="处罚">{{ breakdown.penaltyAmount }}</el-descriptions-item>
        <el-descriptions-item label="应发">{{ breakdown.gross }}</el-descriptions-item>
        <el-descriptions-item label="扣款合计">{{ breakdown.deduction }}</el-descriptions-item>
        <el-descriptions-item label="实发">
          <span style="font-weight: 700; color: #67c23a">{{ breakdown.net }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-alert type="info" :closable="false" style="margin-top: 12px"
                title="口径：日薪=基本工资/21.75，时薪=日薪/8；加班费=审批通过时数×时薪×1.5；缺勤与事假按天扣款；奖惩取自当月登记。生成后写入薪资记录。" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { payrollApi } from '@/api/hr'
import { employeeApi } from '@/api/hr'

const period = ref('2026-11')
const employeeId = ref('')
const employees = ref<Array<{ label: string; value: string }>>([])
const breakdown = ref<any>(null)

async function calculate() {
  if (!employeeId.value) {
    ElMessage.warning('请选择员工')
    return
  }
  breakdown.value = await payrollApi.calculate(period.value, employeeId.value)
}

async function generate() {
  await payrollApi.generate(period.value, employeeId.value, '页面生成')
  ElMessage.success('已生成薪资记录，可在薪资管理中查看')
}

onMounted(async () => {
  employees.value = (await employeeApi.page({ pageNo: 1, pageSize: 100 }))
    .list.map((e: any) => ({ label: e.name, value: e.id }))
})
</script>
