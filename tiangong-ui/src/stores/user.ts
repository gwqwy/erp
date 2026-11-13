import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userId = ref('')
  const username = ref('')
  const nickname = ref('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const fetched = ref(false)

  async function login(form: { username: string; password: string }) {
    const result = await authApi.login(form)
    token.value = result.accessToken
    setToken(result.accessToken)
  }

  async function fetchInfo() {
    const info = await authApi.getPermissionInfo()
    userId.value = info.user.id
    username.value = info.user.username
    nickname.value = info.user.nickname
    roles.value = info.roles
    permissions.value = info.permissions
    fetched.value = true
    return info
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      reset()
    }
  }

  function reset() {
    token.value = ''
    fetched.value = false
    roles.value = []
    permissions.value = []
    removeToken()
  }

  function hasPermission(permission: string): boolean {
    return permissions.value.includes('*:*:*') || permissions.value.includes(permission)
  }

  return { token, userId, username, nickname, roles, permissions, fetched, login, fetchInfo, logout, reset, hasPermission }
})
