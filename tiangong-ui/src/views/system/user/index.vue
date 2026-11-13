<template>
  <CrudPage :config="config">
    <template #actions="{ row }">
      <el-button v-perm="'system:user:assign-role'" link type="primary" @click="openRole(row)">分配角色</el-button>
      <el-button v-perm="'system:user:update'" link type="primary" @click="openPwd(row)">改密</el-button>
    </template>
  </CrudPage>

  <el-dialog v-model="roleVisible" title="分配角色" width="420px">
    <el-select v-model="selectedRoleIds" multiple style="width: 100%" placeholder="选择角色">
      <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
    </el-select>
    <template #footer>
      <el-button @click="roleVisible = false">取消</el-button>
      <el-button type="primary" @click="saveRole">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="pwdVisible" title="重置密码" width="420px">
    <el-input v-model="newPassword" placeholder="新密码" show-password />
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button type="primary" @click="savePwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { deptApi, roleApi, userApi } from '@/api/system'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]

const config: CrudConfig = {
  title: '用户',
  permission: 'system:user',
  api: { page: userApi.page, create: userApi.create, update: userApi.update, delete: userApi.delete },
  fields: [
    { prop: 'username', label: '用户名', required: true, disabledOnUpdate: true, inSearch: true, width: 140 },
    { prop: 'password', label: '密码', type: 'password', required: true, createOnly: true, inTable: false },
    { prop: 'nickname', label: '昵称', required: true, inSearch: true },
    {
      prop: 'deptId',
      label: '部门',
      type: 'select',
      options: async () => (await deptApi.list()).map((d: any) => ({ label: d.name, value: d.id }))
    },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 0, inSearch: true, width: 90 },
    { prop: 'createTime', label: '创建时间', type: 'date', width: 170 }
  ]
}

// ===== 分配角色 =====
const roleVisible = ref(false)
const roleOptions = ref<any[]>([])
const selectedRoleIds = ref<string[]>([])
const currentUserId = ref('')

async function openRole(row: any) {
  currentUserId.value = row.id
  if (!roleOptions.value.length) {
    roleOptions.value = await roleApi.listSimple()
  }
  selectedRoleIds.value = await userApi.getRoleIds(row.id)
  roleVisible.value = true
}

async function saveRole() {
  await userApi.assignRole({ id: currentUserId.value, roleIds: selectedRoleIds.value })
  ElMessage.success('分配成功')
  roleVisible.value = false
}

// ===== 重置密码 =====
const pwdVisible = ref(false)
const newPassword = ref('')

function openPwd(row: any) {
  currentUserId.value = row.id
  newPassword.value = ''
  pwdVisible.value = true
}

async function savePwd() {
  if (!newPassword.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  await userApi.updatePassword({ id: currentUserId.value, password: newPassword.value })
  ElMessage.success('密码已重置')
  pwdVisible.value = false
}
</script>
