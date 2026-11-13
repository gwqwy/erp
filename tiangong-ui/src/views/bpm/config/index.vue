<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { bpmApi } from '@/api/bpm'
import { get } from '@/api/http'

const BIZ_TYPES = [
  { label: '采购订单', value: 'pur_order' },
  { label: '采购入库', value: 'pur_inbound' },
  { label: '销售订单', value: 'sal_order' },
  { label: '销售出库', value: 'sal_outbound' },
  { label: '请假申请', value: 'hr_leave' }
]

const userOptions = async () =>
  (await get<any>('/admin-api/system/user/page', { pageNo: 1, pageSize: 100 })).list
    .map((u: any) => ({ label: `${u.nickname}（${u.username}）`, value: u.id }))

const config: CrudConfig = {
  title: '审批配置',
  permission: 'bpm:config',
  bodyAsQuery: true,
  api: { page: bpmApi.configPage, create: bpmApi.configCreate, update: bpmApi.configUpdate, delete: bpmApi.configDelete },
  fields: [
    { prop: 'bizType', label: '业务类型', type: 'select', required: true, width: 140, disabledOnUpdate: true, options: BIZ_TYPES },
    { prop: 'level', label: '审批级次', type: 'number', required: true, width: 100 },
    { prop: 'approverUserId', label: '审批人', type: 'select', required: true, width: 180, options: userOptions },
    { prop: 'enabled', label: '启用', type: 'select', width: 90, successValue: 1,
      options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }] }
  ]
}
</script>
