<template>
  <CrudPage :config="config">
    <template #actions="{ row }">
      <el-button link type="primary" @click="openFollow(row)">跟进</el-button>
      <el-button v-if="row.stage < 5" v-perm="'crm:business:update'" link type="warning"
                 @click="openStage(row)">推进阶段</el-button>
    </template>
  </CrudPage>

  <!-- 阶段推进 -->
  <el-dialog v-model="stageVisible" title="推进商机阶段" width="440px">
    <el-steps :active="Number(stageRow?.stage || 1) - 1" align-center finish-status="success"
              style="margin-bottom: 16px">
      <el-step title="初步接触" />
      <el-step title="需求确认" />
      <el-step title="方案报价" />
      <el-step title="商务谈判" />
      <el-step title="赢单" />
    </el-steps>
    <el-radio-group v-model="stageValue">
      <el-radio-button v-for="s in stageOptions" :key="s.value" :value="s.value">{{ s.label }}</el-radio-button>
    </el-radio-group>
    <template #footer>
      <el-button @click="stageVisible = false">取消</el-button>
      <el-button type="primary" @click="saveStage">确定</el-button>
    </template>
  </el-dialog>

  <!-- 跟进记录 -->
  <el-drawer v-model="followVisible" :title="`跟进记录 · ${followTarget?.name || ''}`" size="420px">
    <BizChatter biz-type="crm_business" :biz-id="followTarget?.id || ''" />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import BizChatter from '@/components/BizChatter.vue'
import { businessApi } from '@/api/crm'
import { partnerApi } from '@/api/bas'

const stageOptions = [
  { label: '初步接触', value: 1 },
  { label: '需求确认', value: 2 },
  { label: '方案报价', value: 3 },
  { label: '商务谈判', value: 4 },
  { label: '赢单', value: 5 },
  { label: '输单', value: 6 }
]

const customerOptionsLoader = async () =>
  (await partnerApi.page({ pageNo: 1, pageSize: 200, isCustomer: true, status: 0 }))
    .list.map((p: any) => ({ label: p.name, value: p.id }))

const config: CrudConfig = {
  title: '商机',
  permission: 'crm:business',
  api: {
    page: businessApi.page,
    create: businessApi.create,
    update: businessApi.update,
    delete: businessApi.delete
  },
  fields: [
    { prop: 'name', label: '商机名称', required: true, inSearch: true },
    {
      prop: 'partnerId',
      label: '客户',
      type: 'select',
      required: true,
      options: customerOptionsLoader
    },
    { prop: 'amount', label: '预计金额', type: 'number', precision: 2, width: 120 },
    { prop: 'stage', label: '阶段', type: 'select', options: stageOptions, inSearch: true, width: 110 },
    { prop: 'expectDate', label: '预计成交日', type: 'date', width: 120 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

// ===== 阶段推进 =====
const stageVisible = ref(false)
const stageRow = ref<any>(null)
const stageValue = ref(1)

function openStage(row: any) {
  stageRow.value = row
  stageValue.value = Math.min(5, Number(row.stage) + 1)
  stageVisible.value = true
}

async function saveStage() {
  await businessApi.updateStage(stageRow.value.id, stageValue.value)
  ElMessage.success('阶段已更新')
  stageVisible.value = false
  location.reload()
}

// ===== 跟进 =====
const followVisible = ref(false)
const followTarget = ref<{ id: string; name: string } | null>(null)

function openFollow(row: any) {
  followTarget.value = { id: row.id, name: row.name }
  followVisible.value = true
}
</script>
