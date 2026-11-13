<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="login-title">天工 ERP</div>
      <div class="login-subtitle">企业资源计划管理系统</div>
      <el-form :model="form" :rules="rules" ref="formRef" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tip">默认账号：admin / admin123</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useMenuStore } from '@/stores/menu'

const router = useRouter()
const userStore = useUserStore()
const menuStore = useMenuStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await userStore.login(form)
    const info = await userStore.fetchInfo()
    menuStore.setMenus(info.menus)
    menuStore.buildRoutes(router)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #283c52 50%, #3a5169 100%);
}
.login-card { width: 400px; padding: 12px 8px; }
.login-title {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 4px;
}
.login-subtitle {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin: 8px 0 24px;
}
.login-btn { width: 100%; }
.login-tip { text-align: center; color: #c0c4cc; font-size: 12px; }
</style>
