<template>
  <CrudPage :config="config">
    <template #actions="{ row }">
      <el-button link type="primary" @click="openFollow(row)">跟进</el-button>
      <el-button v-if="row.status === 1" v-perm="'crm:lead:convert'" link type="warning"
                 @click="openConvert(row)">转商机</el-button>
    </template>
  </CrudPage>

  <!-- 转商机 -->
  <el-dialog v-model="convertVisible" title="线索转商机" width="480px">
    <el-form label-width="90px">
      <el-form-item label="关联客户" required>
        <el-select v-model="convertForm.partnerId" filterable style="width: 100%">
          <el-option v-for="o in customerOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="预计金额">
        <el-input-number v-model="convertForm.amount" :min="0" :precision="2" :controls="false" style="width: 100%" />
      </el-form-item>
      <el-form-item label="预计成交日">
        <el-date-picker v-model="convertForm.expectDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="convertVisible = false">取消</el-button>
      <el-button type="primary" @click="saveConvert">确定</el-button>
    </template>
  </el-dialog>

  <!-- 跟进记录 -->
  <el-drawer v-model="followVisible" :title="`跟进记录 · ${followTarget?.name || ''}`" size="420px">
    <BizChatter :biz-type="followTarget?.type || 'crm_lead'" :biz-id="followTarget?.id || ''" />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import BizChatter from '@/components/BizChatter.vue'
import { leadApi } from '@/api/crm'
import { partnerApi } from '@/api/bas'

const statusOptions = [
  { label: '待跟进', value: 1 },
  { label: '已转商机', value: 2 },
  { label: '无效', value: 3 }
]

const config: CrudConfig = {
  title: '销售线索',
  permission: 'crm:lead',
  api: { page: leadApi.page, create: leadApi.create, update: leadApi.update, delete: leadApi.delete },
  fields: [
    { prop: 'name', label: '线索名称', required: true, inSearch: true },
    { prop: 'source', label: '来源', width: 100 },
    { prop: 'contact', label: '联系人', width: 110 },
    { prop: 'phone', label: '电话', width: 130 },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 2, inSearch: true, width: 100 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

// ===== 转商机 =====
const convertVisible = ref(false)
const convertForm = ref({ id: '', partnerId: '', amount: 0, expectDate: '' })
const customerOptions = ref<Array<{ label: string; value: string }>>([])

async function openConvert(row: any) {
  convertForm.value = { id: row.id, partnerId: '', amount: 0, expectDate: '' }
  if (!customerOptions.value.length) {
    customerOptions.value = (await partnerApi.page({ pageNo: 1, pageSize: 200, isCustomer: true, status: 0 }))
      .list.map((p: any) => ({ label: p.name, value: p.id }))
  }
  convertVisible.value = true
}

async function saveConvert() {
  if (!convertForm.value.partnerId) {
    ElMessage.warning('请选择关联客户')
    return
  }
  const bizId = await leadApi.convert({
    id: convertForm.value.id,
    partnerId: convertForm.value.partnerId,
    amount: convertForm.value.amount || undefined,
    expectDate: convertForm.value.expectDate || undefined
  })
  ElMessage.success(`已转商机（id ${bizId}），可在商机管理中继续跟进`)
  convertVisible.value = false
  location.reload()
}

// ===== 跟进 =====
const followVisible = ref(false)
const followTarget = ref<{ type: string; id: string; name: string } | null>(null)

function openFollow(row: any) {
  followTarget.value = { type: 'crm_lead', id: row.id, name: row.name }
  followVisible.value = true
}
</script>
