<template>
  <div class="page-card">
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form v-if="searchFields.length" inline class="search-form" @submit.prevent>
        <el-form-item v-for="f in searchFields" :key="f.prop" :label="f.label">
          <template v-if="f.type === 'select'">
            <el-select v-model="searchModel[f.prop]" :placeholder="`请选择${f.label}`" clearable style="width: 180px">
              <el-option v-for="o in resolveOptions(f)" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </template>
          <el-input v-else v-model="searchModel[f.prop]" :placeholder="f.label" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 工具栏 -->
      <div class="table-toolbar">
        <span style="font-weight: 600">{{ config.title }}</span>
        <div>
          <el-button v-perm="config.permission + ':create'" type="primary" :icon="Plus" @click="openCreate">
            新增
          </el-button>
          <el-button :icon="Refresh" circle @click="loadList" />
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column v-for="f in tableFields" :key="f.prop" :label="f.label" :width="f.width">
          <template #default="{ row }">
            <el-tag v-if="hasOptions(f) && row[f.prop] !== null && row[f.prop] !== undefined && row[f.prop] !== ''"
                    :type="tagType(f, row[f.prop])">
              {{ optionLabel(f, row[f.prop]) }}
            </el-tag>
            <span v-else>{{ formatCell(f, row[f.prop]) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" :width="actionWidth" class-name="op-cell">
          <template #default="{ row }">
            <div class="op-group">
            <slot name="actions" :row="row" />
            <el-button v-perm="config.permission + ':update'" link type="primary" @click="openUpdate(row)">
              编辑
            </el-button>
            <el-button v-perm="config.permission + ':delete'" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 12px; justify-content: flex-end"
        @change="loadList"
      />

      <!-- 新增/编辑弹窗 -->
      <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? `新增${config.title}` : `编辑${config.title}`" width="640px">
        <el-form :model="formModel" :rules="formRules" ref="formRef" label-width="120px">
          <el-row>
            <el-col v-for="f in formFields" :key="f.prop" :span="f.span ?? 12">
              <el-form-item :label="f.label" :prop="f.prop">
                <el-switch v-if="f.type === 'switch'" v-model="formModel[f.prop]" />
                <el-input-number v-else-if="f.type === 'number'" v-model="formModel[f.prop]"
                                 :precision="f.precision" :controls="false" style="width: 100%" />
                <el-select v-else-if="f.type === 'select'" v-model="formModel[f.prop]" style="width: 100%" clearable>
                  <el-option v-for="o in resolveOptions(f)" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
                <el-input v-else-if="f.type === 'textarea'" v-model="formModel[f.prop]" type="textarea" :rows="2" />
                <el-input v-else v-model="formModel[f.prop]" :type="f.type === 'password' ? 'password' : 'text'"
                          :show-password="f.type === 'password'"
                          :disabled="dialogMode === 'update' && f.disabledOnUpdate" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, useSlots } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

export interface CrudField {
  prop: string
  label: string
  type?: 'input' | 'password' | 'number' | 'textarea' | 'switch' | 'select' | 'date'
  options?: Array<{ label: string; value: any }> | (() => Promise<Array<{ label: string; value: any }>>)
  inSearch?: boolean
  inForm?: boolean
  inTable?: boolean
  required?: boolean
  width?: number | string
  precision?: number
  disabledOnUpdate?: boolean
  createOnly?: boolean
  successValue?: any
  span?: number
}

export interface CrudConfig {
  title: string
  permission: string
  api: {
    page: (params: any) => Promise<any>
    /** body 风格传表单对象；bodyAsQuery 时传 query 参数对象 */
    create: (dataOrParams: any) => Promise<any>
    update: (dataOrParams: any) => Promise<any>
    delete: (id: any) => Promise<any>
  }
  fields: CrudField[]
  /** 后端 @RequestParam 风格时置 true：create/update 以 query 参数发送 */
  bodyAsQuery?: boolean
}

const props = defineProps<{ config: CrudConfig }>()

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentId = ref<any>(null)
const formRef = ref<FormInstance>()
const formModel = reactive<Record<string, any>>({})
const searchModel = reactive<Record<string, any>>({})
const optionStore = reactive<Record<string, Array<{ label: string; value: any }>>>({})

const searchFields = computed(() => props.config.fields.filter(f => f.inSearch))
const tableFields = computed(() => props.config.fields.filter(f => f.inTable !== false))
const slots = useSlots()
const actionWidth = computed(() => 240 + 70 * (slots.actions ? 1 : 0))

const formFields = computed(() =>
  props.config.fields.filter(f => {
    if (f.inForm === false) return false
    if (f.createOnly && dialogMode.value === 'update') return false
    return true
  })
)

const formRules = computed<FormRules>(() => {
  const rules: FormRules = {}
  for (const f of formFields.value) {
    if (f.required) {
      rules[f.prop] = [{ required: true, message: `${f.label}不能为空`, trigger: 'blur' }]
    }
  }
  return rules
})

function hasOptions(f: CrudField) {
  return f.type === 'select' || Boolean(f.options) || (f.type === 'switch' && Boolean(f.options))
}

function resolveOptions(f: CrudField) {
  return optionStore[f.prop] ?? (Array.isArray(f.options) ? f.options : [])
}

function optionLabel(f: CrudField, value: any) {
  const options = resolveOptions(f)
  const hit = options.find(o => String(o.value) === String(value))
  return hit ? hit.label : value
}

function tagType(f: CrudField, value: any) {
  if (f.successValue !== undefined && String(f.successValue) === String(value)) return 'success'
  if (value === 0 || value === true) return 'success'
  return 'info'
}

function formatCell(f: CrudField, value: any) {
  if (value === null || value === undefined) return '—'
  if (f.type === 'date') return String(value).replace('T', ' ')
  if (typeof value === 'boolean') return value ? '是' : '否'
  return value
}

async function loadList() {
  loading.value = true
  try {
    const params: any = { pageNo: pageNo.value, pageSize: pageSize.value, ...searchModel }
    const res = await props.config.api.page(params)
    list.value = res?.list ?? []
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  for (const f of searchFields.value) searchModel[f.prop] = undefined
  pageNo.value = 1
  loadList()
}

function defaultFor(f: CrudField) {
  if (f.type === 'switch') return false
  if (f.type === 'number') return undefined
  return undefined
}

function openCreate() {
  dialogMode.value = 'create'
  currentId.value = null
  for (const f of props.config.fields) formModel[f.prop] = defaultFor(f)
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openUpdate(row: any) {
  dialogMode.value = 'update'
  currentId.value = row.id
  for (const f of props.config.fields) {
    formModel[f.prop] = row[f.prop] ?? defaultFor(f)
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (props.config.bodyAsQuery) {
      const params: Record<string, any> = { ...formModel }
      if (dialogMode.value === 'update') params.id = currentId.value
      if (dialogMode.value === 'create') await props.config.api.create(params)
      else await props.config.api.update(params)
    } else {
      if (dialogMode.value === 'create') await props.config.api.create({ ...formModel })
      else await props.config.api.update({ id: currentId.value, ...formModel })
    }
    ElMessage.success(dialogMode.value === 'create' ? '新增成功' : '修改成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除该条${props.config.title}吗？`, '提示', { type: 'warning' })
  await props.config.api.delete(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(async () => {
  loadList()
  // 解析异步下拉选项
  for (const f of props.config.fields) {
    if (typeof f.options === 'function') {
      optionStore[f.prop] = await f.options()
    }
  }
})
</script>
