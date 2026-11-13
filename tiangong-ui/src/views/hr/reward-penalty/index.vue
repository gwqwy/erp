<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { rewardPenaltyApi } from '@/api/hr'
import { employeeApi } from '@/api/hr'

const typeOptions = [
  { label: '奖励', value: 'reward' },
  { label: '处罚', value: 'penalty' }
]

const config: CrudConfig = {
  title: '奖惩记录',
  permission: 'hr:reward',
  bodyAsQuery: true,
  api: { page: rewardPenaltyApi.page, create: rewardPenaltyApi.create, delete: rewardPenaltyApi.delete },
  fields: [
    { prop: 'no', label: '单号', width: 150, inForm: false },
    { prop: 'employeeId', label: '员工', type: 'select', required: true, width: 130,
      options: async () => (await employeeApi.page({ pageNo: 1, pageSize: 100 })).list
        .map((e: any) => ({ label: e.name, value: e.id })) },
    { prop: 'type', label: '类型', type: 'select', required: true, width: 90, options: typeOptions },
    { prop: 'amount', label: '金额', type: 'number', precision: 2, width: 110 },
    { prop: 'rpDate', label: '日期', type: 'date', width: 110 },
    { prop: 'reason', label: '事由', type: 'textarea', span: 24, inTable: false }
  ]
}
</script>
