<template>
  <CrudPage :config="config">
    <template #actions="{ row }">
      <el-button v-if="row.status === 0" link type="success" @click="markDone(row)">确认回款</el-button>
    </template>
  </CrudPage>
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { ElMessage } from 'element-plus'
import { receivablePlanApi } from '@/api/fin'
import { partnerApi } from '@/api/bas'

const statusOptions = [
  { label: '计划中', value: 0 },
  { label: '已回款', value: 1 }
]

const config: CrudConfig = {
  title: '回款计划',
  permission: 'fin:receivableplan',
  bodyAsQuery: true,
  api: { page: pagedList, create: receivablePlanApi.create, delete: receivablePlanApi.delete },
  fields: [
    { prop: 'no', label: '计划号', width: 150, inForm: false },
    { prop: 'partnerId', label: '客户', type: 'select', required: true, width: 150,
      options: async () => (await partnerApi.page({ pageNo: 1, pageSize: 200 })).list
        .map((p: any) => ({ label: p.name, value: p.id })) },
    { prop: 'planDate', label: '计划回款日', type: 'date', required: true, width: 130 },
    { prop: 'amount', label: '金额', type: 'number', required: true, width: 120 },
    { prop: 'status', label: '状态', type: 'select', width: 100, successValue: 1, options: statusOptions },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(params: any) {
  const list = await receivablePlanApi.list({ partnerId: params.partnerId })
  return { list, total: list.length }
}

async function markDone(row: any) {
  await receivablePlanApi.done(row.id)
  ElMessage.success('已确认回款')
  config.api.page({ pageNo: 1, pageSize: 20 })
}
</script>
