<template>
  <CrudPage :config="config">
    <template #actions="{ row }">
      <el-button v-perm="'system:role:assign-menu'" link type="primary" @click="openMenu(row)">分配菜单</el-button>
    </template>
  </CrudPage>

  <el-dialog v-model="menuVisible" title="分配菜单" width="420px">
    <el-tree
      ref="menuTreeRef"
      :data="menuTree"
      node-key="id"
      show-checkbox
      default-expand-all
      :props="{ label: 'name', children: 'children' }"
    />
    <template #footer>
      <el-button @click="menuVisible = false">取消</el-button>
      <el-button type="primary" @click="saveMenu">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import type { ElTree } from 'element-plus'
import { ElMessage } from 'element-plus'
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { menuApi, roleApi } from '@/api/system'
import { buildTree } from '@/utils/tree'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]

const config: CrudConfig = {
  title: '角色',
  permission: 'system:role',
  api: { page: roleApi.page, create: roleApi.create, update: roleApi.update, delete: roleApi.delete },
  fields: [
    { prop: 'name', label: '角色名称', required: true, inSearch: true },
    { prop: 'code', label: '角色编码', required: true, disabledOnUpdate: true },
    { prop: 'sort', label: '排序', type: 'number', width: 90 },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 0, width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

// ===== 分配菜单 =====
const menuVisible = ref(false)
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const menuTree = ref<any[]>([])
const currentRoleId = ref('')

async function openMenu(row: any) {
  currentRoleId.value = row.id
  if (!menuTree.value.length) {
    const flat = await menuApi.list()
    menuTree.value = buildTree(flat)
  }
  const checked = await roleApi.getMenuIds(row.id)
  menuVisible.value = true
  await nextTick()
  // 只勾选叶子（按钮），父节点由级联自动半选/全选
  const parentIds = new Set(flatParentIds())
  menuTreeRef.value?.setCheckedKeys(checked.filter((id: any) => !parentIds.has(String(id))))
}

function flatParentIds(): Set<string> {
  const ids = new Set<string>()
  const walk = (nodes: any[]) => {
    for (const n of nodes) {
      if (n.children?.length) {
        ids.add(String(n.id))
        walk(n.children)
      }
    }
  }
  walk(menuTree.value)
  return ids
}

async function saveMenu() {
  const checked = menuTreeRef.value?.getCheckedKeys() ?? []
  const half = menuTreeRef.value?.getHalfCheckedKeys() ?? []
  await roleApi.assignMenu({ roleId: currentRoleId.value, menuIds: [...checked, ...half] })
  ElMessage.success('分配成功')
  menuVisible.value = false
}
</script>
