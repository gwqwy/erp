<template>
  <CrudPage :config="config">
    <template #actions="{ row }">
      <el-button link type="primary" @click="preview(row)">预览</el-button>
    </template>
  </CrudPage>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { serialRuleApi } from '@/api/bas'

const statuslessResetOptions = [
  { label: '不重置', value: 'none' },
  { label: '按年', value: 'year' },
  { label: '按月', value: 'month' },
  { label: '按天', value: 'day' }
]

const config: CrudConfig = {
  title: '单号规则',
  permission: 'bas:serial-rule',
  bodyAsQuery: true,
  api: { page: pagedList, create: serialRuleApi.create, update: serialRuleApi.update, delete: serialRuleApi.delete },
  fields: [
    { prop: 'bizType', label: '业务类型', required: true, disabledOnUpdate: true, width: 160 },
    { prop: 'prefix', label: '前缀', required: true, width: 90 },
    { prop: 'dateFormat', label: '日期格式', width: 120 },
    { prop: 'seqLength', label: '序列位数', type: 'number', width: 100 },
    { prop: 'resetType', label: '重置周期', type: 'select', options: statuslessResetOptions, width: 110 },
    { prop: 'remark', label: '备注', span: 24, inTable: false }
  ]
}

async function pagedList(_params: any) {
  const list = await serialRuleApi.list()
  return { list, total: list.length }
}

async function preview(row: any) {
  const next = await serialRuleApi.preview(row.bizType)
  ElMessage.info(`下一个单号：${next}`)
}
</script>
