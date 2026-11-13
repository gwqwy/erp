<template>
  <CrudPage :config="config">
    <template #actions="{ row }">
      <el-button v-if="row.status === 10" v-perm="'fin:expense:audit'" link type="warning"
                 @click="audit(row)">审核</el-button>
    </template>
  </CrudPage>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { expenseApi } from '@/api/more'

const categoryOptions = ['办公', '差旅', '租金', '水电', '其他'].map(v => ({ label: v, value: v }))
const statusOptions = [
  { label: '草稿', value: 10 },
  { label: '已审核', value: 30 },
  { label: '已作废', value: 50 }
]

const config: CrudConfig = {
  title: '费用登记',
  permission: 'fin:expense',
  bodyAsQuery: true,
  api: { page: expenseApi.page, create: expenseApi.create, delete: expenseApi.delete },
  fields: [
    { prop: 'no', label: '单号', width: 150, inForm: false },
    { prop: 'category', label: '费用分类', type: 'select', required: true, width: 120, inSearch: true, options: categoryOptions },
    { prop: 'amount', label: '金额', type: 'number', precision: 2, required: true, width: 120 },
    {
      prop: 'accountId',
      label: '支付账户',
      type: 'select',
      required: true,
      width: 160,
      options: async () => (await expenseApi.accountList()).map(a => ({ label: a.name, value: a.id }))
    },
    { prop: 'expenseDate', label: '费用日期', type: 'date', width: 120 },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, inSearch: true, width: 100 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function audit(row: any) {
  await ElMessageBox.confirm('审核后生成费用凭证并从账户扣款，确认？', '提示', { type: 'warning' })
  await expenseApi.audit(row.id)
  ElMessage.success('审核通过')
  location.reload()
}
</script>
