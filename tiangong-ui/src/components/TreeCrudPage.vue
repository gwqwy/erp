<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">{{ config.title }}</span>
        <div>
          <el-button v-perm="config.permission + ':create'" type="primary" :icon="Plus" @click="openCreate(null)">
            新增
          </el-button>
          <el-button :icon="Refresh" circle @click="loadList" />
        </div>
      </div>

      <el-table :data="treeData" v-loading="loading" border row-key="id"
                :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column v-for="f in tableFields" :key="f.prop" :label="f.label" :width="f.width"
                         :min-width="f.prop === parentField?.prop ? 220 : undefined">
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
            <el-button v-perm="config.permission + ':create'" link type="primary" @click="openCreate(row)">
              新增子级
            </el-button>
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

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
        <el-form :model="formModel" :rules="formRules" ref="formRef" label-width="120px">
          <el-form-item v-for="f in formFields" :key="f.prop" :label="f.label" :prop="f.prop">
            <el-tree-select
              v-if="f.prop === parentField?.prop"
              v-model="formModel[f.prop]"
              :data="parentOptions"
              :props="{ label: config.parentLabel || 'name', value: 'id', children: 'children' }"
              node-key="id"
              check-strictly
              clearable
              style="width: 100%"
              placeholder="不选择则为顶级"
            />
            <el-switch v-else-if="f.type === 'switch'" v-model="formModel[f.prop]" />
            <el-input-number v-else-if="f.type === 'number'" v-model="formModel[f.prop]"
                             :precision="f.precision" :controls="false" style="width: 100%" />
            <el-select v-else-if="f.type === 'select'" v-model="formModel[f.prop]" style="width: 100%">
              <el-option v-for="o in resolveOptions(f)" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <el-input v-else v-model="formModel[f.prop]" />
          </el-form-item>
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
import { Plus, Refresh } from '@element-plus/icons-vue'
import { buildTree } from '@/utils/tree'
import type { CrudField } from './CrudPage.vue'

export interface TreeCrudConfig {
  title: string
  permission: string
  api: {
    list: (params?: any) => Promise<any[]>
    /** body 风格传表单对象；bodyAsQuery 时传 query 参数对象 */
    create: (dataOrParams: any) => Promise<any>
    update: (dataOrParams: any) => Promise<any>
    delete: (id: any) => Promise<any>
  }
  fields: CrudField[]
  /** 树节点展示字段名，默认 name */
  parentLabel?: string
  /** 后端 @RequestParam 风格时置 true */
  bodyAsQuery?: boolean
}

const props = defineProps<{ config: TreeCrudConfig }>()
const slots = useSlots()

const loading = ref(false)
const saving = ref(false)
const flatList = ref<any[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentId = ref<any>(null)
const formRef = ref<FormInstance>()
const formModel = reactive<Record<string, any>>({})
const optionStore = reactive<Record<string, Array<{ label: string; value: any }>>>({})

const parentField = computed(() => props.config.fields.find(f => f.prop === 'parentId'))
const tableFields = computed(() => props.config.fields.filter(f => f.inTable !== false))
const formFields = computed(() => props.config.fields.filter(f => f.inForm !== false))
const actionWidth = computed(() => (slots.actions ? 80 : 0) + 250)
const dialogTitle = computed(() =>
  dialogMode.value === 'create' ? `新增${props.config.title}` : `编辑${props.config.title}`
)

const treeData = computed(() => buildTree(flatList.value))

/** 父节点可选项：编辑时排除自身及其后代（扁平化过滤后再建树） */
const parentOptions = computed(() => {
  if (dialogMode.value !== 'update') return treeData.value
  const exclude = new Set<any>([currentId.value])
  const collect = (nodes: any[]) => {
    for (const n of nodes) {
      exclude.add(n.id)
      if (n.children) collect(n.children)
    }
  }
  collect(treeData.value.filter(n => n.id === currentId.value))
  return buildTree(flatList.value.filter(item => !exclude.has(item.id)))
})

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
  if (value === null || value === undefined || value === '') {
    return f.prop === parentField?.prop ? '顶级' : '—'
  }
  if (f.prop === parentField?.prop) {
    const hit = flatList.value.find(item => String(item.id) === String(value))
    return hit ? hit[props.config.parentLabel || 'name'] : value
  }
  if (typeof value === 'boolean') return value ? '是' : '否'
  return value
}

async function loadList() {
  loading.value = true
  try {
    flatList.value = (await props.config.api.list()) ?? []
  } finally {
    loading.value = false
  }
}

function resetForm(parentId: any) {
  for (const f of props.config.fields) {
    if (f.prop === 'parentId') formModel[f.prop] = parentId ?? undefined
    else if (f.type === 'switch') formModel[f.prop] = false
    else if (f.type === 'number') formModel[f.prop] = f.prop === 'sort' ? 0 : undefined
    else formModel[f.prop] = undefined
  }
}

function openCreate(row: any) {
  dialogMode.value = 'create'
  currentId.value = null
  resetForm(row ? row.id : null)
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openUpdate(row: any) {
  dialogMode.value = 'update'
  currentId.value = row.id
  for (const f of props.config.fields) {
    formModel[f.prop] = row[f.prop] ?? undefined
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const parentId = formModel.parentId ?? 0
    if (props.config.bodyAsQuery) {
      const params: Record<string, any> = { ...formModel, parentId }
      if (dialogMode.value === 'create') await props.config.api.create(params)
      else await props.config.api.update(params)
    } else {
      if (dialogMode.value === 'create') await props.config.api.create({ ...formModel, parentId })
      else await props.config.api.update({ id: currentId.value, ...formModel, parentId })
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
  for (const f of props.config.fields) {
    if (typeof f.options === 'function') {
      optionStore[f.prop] = await f.options()
    }
  }
})
</script>
