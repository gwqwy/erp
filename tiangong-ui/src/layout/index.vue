<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">天工 ERP</div>
      <el-menu
        :default-active="activeMenu"
        router
        unique-opened
        background-color="#001529"
        text-color="#c8c9cc"
        active-text-color="#ffffff"
        class="aside-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <template v-for="dir in menuStore.menus" :key="dir.id">
          <el-sub-menu :index="dir.path">
            <template #title>{{ dir.name }}</template>
            <el-menu-item
              v-for="child in (dir.children || []).filter(c => c.type === 2)"
              :key="child.id"
              :index="resolvePath(dir.path, child.path)"
            >
              {{ child.name }}
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="$route.meta?.title">{{ $route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown @command="onCommand">
          <span class="user-entry">
            <el-icon><UserFilled /></el-icon>
            {{ userStore.nickname || userStore.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMenuStore } from '@/stores/menu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const menuStore = useMenuStore()

const activeMenu = computed(() => route.path)

function resolvePath(dirPath: string, childPath: string) {
  return `${dirPath}/${childPath}`.replace(/\/+/g, '/')
}

async function onCommand(command: string) {
  if (command === 'logout') {
    await userStore.logout()
    menuStore.reset()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100%; }
.aside { background-color: #001529; display: flex; flex-direction: column; }
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
  background-color: #001529;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.aside-menu { border-right: none; flex: 1; overflow-y: auto; }
.header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  height: 56px;
}
.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #303133;
  outline: none;
}
.main { padding: 0; overflow-y: auto; }
</style>
