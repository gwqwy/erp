<!-- 通用"申请类"页面：加班/出差/离职 共用模式（CrudPage + 提交审批/直接审核动作） -->
<template>
  <CrudPage :config="config">
    <template #actions="{ row }">
      <el-button v-if="row.status === 10 && config.api.submitApproval" v-perm="config.permission + ':audit'"
                 link type="warning" @click="submit(row)">提交审批</el-button>
      <el-button v-if="row.status === 10 && config.api.audit" v-perm="config.permission + ':audit'"
                 link type="success" @click="audit(row)">直接审核</el-button>
    </template>
  </CrudPage>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'

const props = defineProps<{ config: CrudConfig }>()

async function submit(row: any) {
  await ElMessageBox.confirm('提交到审批流？', '提示', { type: 'warning' })
  await props.config.api.submitApproval!(row.id)
  ElMessage.success('已提交审批')
  location.reload()
}

async function audit(row: any) {
  await ElMessageBox.confirm('确认直接通过该申请吗？', '提示', { type: 'warning' })
  await props.config.api.audit!(row.id)
  ElMessage.success('审核通过')
  location.reload()
}
</script>
