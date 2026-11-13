<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">动态表单（低代码：JSON Schema 定义 → 免代码填报）</span>
        <el-button v-perm="'infra:form:create'" type="primary" :icon="Plus" @click="openDef">新建表单</el-button>
      </div>
      <el-table :data="defs" v-loading="loading" border stripe>
        <el-table-column prop="name" label="表单名称" min-width="180" />
        <el-table-column label="字段数" width="90" align="center">
          <template #default="{ row }">{{ parseFields(row.fieldsJson).length }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openFill(row)">填报</el-button>
            <el-button link type="primary" @click="openData(row)">数据</el-button>
            <el-button v-perm="'infra:form:create'" link type="danger" @click="removeDef(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 新建表单 -->
      <el-dialog v-model="defVisible" title="新建表单" width="640px">
        <el-form label-width="90px">
          <el-form-item label="表单名称" required>
            <el-input v-model="defName" placeholder="如：设备报修登记" />
          </el-form-item>
          <el-form-item label="字段定义" required>
            <el-input v-model="defFields" type="textarea" :rows="7"
                      placeholder='[{"prop":"device","label":"设备名","type":"input","required":true},{"prop":"issue","label":"故障","type":"textarea"}]' />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="defVisible = false">取消</el-button>
          <el-button type="primary" @click="saveDef">创建</el-button>
        </template>
      </el-dialog>

      <!-- 填报 -->
      <el-dialog v-model="fillVisible" :title="`填报 · ${fillTarget?.name || ''}`" width="520px">
        <el-form label-width="110px">
          <el-form-item v-for="f in fillFields" :key="f.prop" :label="f.label" :required="f.required">
            <el-input-number v-if="f.type === 'number'" v-model="fillModel[f.prop]" :controls="false" style="width: 100%" />
            <el-date-picker v-else-if="f.type === 'date'" v-model="fillModel[f.prop]" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            <el-select v-else-if="f.type === 'select'" v-model="fillModel[f.prop]" style="width: 100%">
              <el-option v-for="o in f.options || []" :key="o" :label="o" :value="o" />
            </el-select>
            <el-input v-else-if="f.type === 'textarea'" v-model="fillModel[f.prop]" type="textarea" :rows="2" />
            <el-input v-else v-model="fillModel[f.prop]" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="fillVisible = false">取消</el-button>
          <el-button type="primary" @click="saveFill">提交</el-button>
        </template>
      </el-dialog>

      <!-- 数据 -->
      <el-drawer v-model="dataVisible" :title="`数据 · ${dataTarget?.name || ''}`" size="620px">
        <el-table :data="dataRows" border size="small">
          <el-table-column v-for="f in dataFields" :key="f.prop" :label="f.label" min-width="120">
            <template #default="{ row }">{{ row.data[f.prop] ?? '—' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" width="160">
            <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
          </el-table-column>
        </el-table>
      </el-drawer>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { formApi } from '@/api/infra'

const loading = ref(false)
const defs = ref<any[]>([])

const defVisible = ref(false)
const defName = ref('')
const defFields = ref('')

const fillVisible = ref(false)
const fillTarget = ref<any>(null)
const fillFields = ref<any[]>([])
const fillModel = reactive<Record<string, any>>({})

const dataVisible = ref(false)
const dataTarget = ref<any>(null)
const dataRows = ref<any[]>([])
const dataFields = ref<any[]>([])

function parseFields(json: string): any[] {
  try { return JSON.parse(json || '[]') } catch { return [] }
}

async function load() {
  loading.value = true
  try {
    defs.value = (await formApi.defList()) ?? []
  } finally {
    loading.value = false
  }
}

function openDef() {
  defName.value = ''
  defFields.value = ''
  defVisible.value = true
}

async function saveDef() {
  try {
    const fields = JSON.parse(defFields.value)
    if (!defName.value || !Array.isArray(fields) || !fields.length) {
      ElMessage.warning('请填写名称与合法的字段 JSON 数组')
      return
    }
    await formApi.defCreate({ name: defName.value, fieldsJson: JSON.stringify(fields) })
    ElMessage.success('创建成功')
    defVisible.value = false
    load()
  } catch {
    ElMessage.error('字段 JSON 格式不合法')
  }
}

function openFill(row: any) {
  fillTarget.value = row
  fillFields.value = parseFields(row.fieldsJson)
  Object.keys(fillModel).forEach(k => delete fillModel[k])
  fillVisible.value = true
}

async function saveFill() {
  const missing = fillFields.value.find(f => f.required && !fillModel[f.prop])
  if (missing) {
    ElMessage.warning(`${missing.label} 为必填`)
    return
  }
  await formApi.dataCreate({ formDefId: fillTarget.value.id, dataJson: JSON.stringify(fillModel) })
  ElMessage.success('提交成功')
  fillVisible.value = false
}

async function openData(row: any) {
  dataTarget.value = row
  dataFields.value = parseFields(row.fieldsJson)
  const res = await formApi.dataPage({ formDefId: row.id, pageNo: 1, pageSize: 50 })
  dataRows.value = res?.list ?? []
  dataVisible.value = true
}

async function removeDef(row: any) {
  await ElMessageBox.confirm(`确认删除表单「${row.name}」？`, '提示', { type: 'warning' })
  await formApi.defDelete(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
