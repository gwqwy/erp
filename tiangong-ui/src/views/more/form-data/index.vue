<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <el-select v-model="formDefId" placeholder="选择表单" style="width: 240px" @change="load">
          <el-option v-for="d in defs" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
        <div>
          <el-button @click="defDialog = true">新建表单定义</el-button>
          <el-button type="primary" :disabled="!formDefId" @click="openFill">填 报</el-button>
        </div>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column v-for="c in columns" :key="c" :prop="'data.' + c" :label="c" min-width="140" />
      <el-table-column prop="createTime" label="填报时间" width="170" />
      <el-table-column label="操作" width="90" class-name="op-cell">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top:12px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="pageNo" :page-size="pageSize" :total="total"
        layout="total, prev, pager, next" @current-change="load" />
    </div>
  </el-card>

  <!-- 表单定义 -->
  <el-dialog v-model="defDialog" title="新建表单定义" width="560px">
    <el-form label-width="90px">
      <el-form-item label="表单名称"><el-input v-model="defName" /></el-form-item>
      <el-form-item label="字段 JSON">
        <el-input v-model="defFields" type="textarea" :rows="6" placeholder='[{"prop":"name","label":"姓名"}]' />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="defDialog = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveDef">保存</el-button>
    </template>
  </el-dialog>

  <!-- 填报 -->
  <el-dialog v-model="fillDialog" :title="'填报 · ' + (currentDef?.name || '')" width="560px">
    <el-form label-width="110px">
      <el-form-item v-for="f in defFieldsParsed" :key="f.prop" :label="f.label || f.prop">
        <el-input v-model="fillModel[f.prop]" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="fillDialog = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveFill">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formDataApi } from '@/api/infra'

const defs = ref<any[]>([])
const formDefId = ref<string>('')
const rows = ref<any[]>([])
const columns = ref<string[]>([])
const loading = ref(false)
const saving = ref(false)
const pageNo = ref(1)
const pageSize = 20
const total = ref(0)

const defDialog = ref(false)
const defName = ref('')
const defFields = ref('[{"prop":"name","label":"名称"}]')

const fillDialog = ref(false)
const fillModel = reactive<Record<string, any>>({})

const currentDef = computed(() => defs.value.find(d => String(d.id) === String(formDefId.value)))
const defFieldsParsed = computed<any[]>(() => {
  try {
    const arr = JSON.parse(currentDef.value?.fieldsJson || '[]')
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

async function loadDefs() {
  defs.value = await formDataApi.defList()
  if (!formDefId.value && defs.value.length) {
    formDefId.value = String(defs.value[0].id)
    await load()
  }
}

async function load() {
  if (!formDefId.value) return
  loading.value = true
  try {
    const page = await formDataApi.dataPage({ pageNo: pageNo.value, pageSize, formDefId: formDefId.value })
    rows.value = page.list || []
    total.value = page.total || 0
    const set = new Set<string>()
    rows.value.forEach((r: any) => Object.keys(r.data || {}).forEach(k => set.add(k)))
    columns.value = [...set]
  } finally {
    loading.value = false
  }
}

async function saveDef() {
  saving.value = true
  try {
    await formDataApi.defCreate({ name: defName.value, fieldsJson: defFields.value })
    ElMessage.success('表单定义已保存')
    defDialog.value = false
    defName.value = ''
    await loadDefs()
  } finally {
    saving.value = false
  }
}

function openFill() {
  Object.keys(fillModel).forEach(k => delete fillModel[k])
  defFieldsParsed.value.forEach((f: any) => (fillModel[f.prop] = ''))
  fillDialog.value = true
}

async function saveFill() {
  saving.value = true
  try {
    await formDataApi.dataCreate({ formDefId: formDefId.value, dataJson: JSON.stringify(fillModel) })
    ElMessage.success('填报成功')
    fillDialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除该条填报数据吗？', '提示', { type: 'warning' })
  await formDataApi.dataDelete(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(loadDefs)
</script>
