<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">字典类型</span>
        <el-button v-perm="'system:dict:create'" type="primary" :icon="Plus" @click="openTypeCreate()">新增</el-button>
      </div>

      <el-table :data="typeList" v-loading="loading" border stripe>
        <el-table-column prop="name" label="字典名称" />
        <el-table-column prop="type" label="类型编码" width="220" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:dict:query'" link type="primary" @click="openData(row)">字典数据</el-button>
            <el-button v-perm="'system:dict:update'" link type="primary" @click="openTypeCreate(row)">编辑</el-button>
            <el-button v-perm="'system:dict:delete'" link type="danger" @click="removeType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNo"
        :total="total"
        :page-size="10"
        layout="total, prev, pager, next"
        style="margin-top: 12px; justify-content: flex-end"
        @current-change="loadTypes"
      />
    </el-card>

    <!-- 类型编辑 -->
    <el-dialog v-model="typeDialogVisible" :title="typeForm.id ? '编辑字典类型' : '新增字典类型'" width="440px">
      <el-form :model="typeForm" label-width="90px">
        <el-form-item label="字典名称" required>
          <el-input v-model="typeForm.name" />
        </el-form-item>
        <el-form-item label="类型编码" required>
          <el-input v-model="typeForm.type" :disabled="!!typeForm.id" placeholder="如 common_status" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据抽屉 -->
    <el-drawer v-model="dataDrawerVisible" :title="`字典数据 · ${currentType?.name || ''}`" size="560px">
      <div class="table-toolbar">
        <span>类型编码：{{ currentType?.type }}</span>
        <el-button v-perm="'system:dict:create'" type="primary" size="small" @click="openDataCreate()">新增数据</el-button>
      </div>
      <el-table :data="dataList" border size="small">
        <el-table-column prop="label" label="标签" />
        <el-table-column prop="value" label="值" />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-perm="'system:dict:update'" link type="primary" @click="openDataCreate(row)">编辑</el-button>
            <el-button v-perm="'system:dict:delete'" link type="danger" @click="removeData(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dataDialogVisible" :title="dataForm.id ? '编辑数据' : '新增数据'" width="420px" append-to-body>
        <el-form :model="dataForm" label-width="70px">
          <el-form-item label="标签" required><el-input v-model="dataForm.label" /></el-form-item>
          <el-form-item label="值" required><el-input v-model="dataForm.value" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="dataForm.sort" :controls="false" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dataDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveData">确定</el-button>
        </template>
      </el-dialog>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { dictApi } from '@/api/system'

const loading = ref(false)
const typeList = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)

const typeDialogVisible = ref(false)
const typeForm = reactive<{ id?: string; name: string; type: string }>({ name: '', type: '' })

const dataDrawerVisible = ref(false)
const currentType = ref<any>(null)
const dataList = ref<any[]>([])
const dataDialogVisible = ref(false)
const dataForm = reactive<{ id?: string; label: string; value: string; sort: number }>({
  label: '',
  value: '',
  sort: 0
})

async function loadTypes() {
  loading.value = true
  try {
    const res = await dictApi.typePage({ pageNo: pageNo.value, pageSize: 10 })
    typeList.value = res?.list ?? []
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

function openTypeCreate(row?: any) {
  typeForm.id = row?.id
  typeForm.name = row?.name ?? ''
  typeForm.type = row?.type ?? ''
  typeDialogVisible.value = true
}

async function saveType() {
  if (!typeForm.name || (!typeForm.type && !typeForm.id)) {
    ElMessage.warning('请填写完整')
    return
  }
  if (typeForm.id) {
    await dictApi.typeUpdate({ id: typeForm.id, name: typeForm.name })
  } else {
    await dictApi.typeCreate({ name: typeForm.name, type: typeForm.type })
  }
  ElMessage.success('保存成功')
  typeDialogVisible.value = false
  loadTypes()
}

async function removeType(row: any) {
  await ElMessageBox.confirm(`确认删除字典类型「${row.name}」吗？`, '提示', { type: 'warning' })
  await dictApi.typeDelete(row.id)
  ElMessage.success('删除成功')
  loadTypes()
}

async function openData(row: any) {
  currentType.value = row
  dataList.value = await dictApi.dataList(row.type)
  dataDrawerVisible.value = true
}

function openDataCreate(row?: any) {
  dataForm.id = row?.id
  dataForm.label = row?.label ?? ''
  dataForm.value = row?.value ?? ''
  dataForm.sort = row?.sort ?? 0
  dataDialogVisible.value = true
}

async function saveData() {
  if (!dataForm.label || !dataForm.value) {
    ElMessage.warning('请填写标签与值')
    return
  }
  if (dataForm.id) {
    await dictApi.dataUpdate({ id: dataForm.id, label: dataForm.label, value: dataForm.value, sort: dataForm.sort })
  } else {
    await dictApi.dataCreate({ dictType: currentType.value.type, label: dataForm.label, value: dataForm.value, sort: dataForm.sort })
  }
  ElMessage.success('保存成功')
  dataDialogVisible.value = false
  dataList.value = await dictApi.dataList(currentType.value.type)
}

async function removeData(row: any) {
  await ElMessageBox.confirm(`确认删除「${row.label}」吗？`, '提示', { type: 'warning' })
  await dictApi.dataDelete(row.id)
  ElMessage.success('删除成功')
  dataList.value = await dictApi.dataList(currentType.value.type)
}

onMounted(loadTypes)
</script>
