import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import Layout from '@/layout/index.vue'

/**
 * 静态路由：登录页 + 主布局（工作台）。
 * 业务路由由 menu store 依据后端菜单树动态注册（见 stores/menu.ts）。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/print/order',
      name: 'PrintOrder',
      component: () => import('@/views/print/order/index.vue'),
      meta: { title: '单据打印' }
    },
    {
      path: '/screen',
      name: 'Screen',
      component: () => import('@/views/screen/index.vue'),
      meta: { title: '经营大屏' }
    },
    {
      path: '/',
      name: 'layout',
      component: Layout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '工作台' }
        }
      ]
    }
  ]
})

router.beforeEach(async to => {
  const token = getToken()
  if (!token) {
    return to.path === '/login' ? true : '/login'
  }
  if (to.path === '/login') {
    return '/dashboard'
  }
  // 首次进入：拉取权限信息并注册动态路由，然后重放当前导航
  const { useUserStore } = await import('@/stores/user')
  const { useMenuStore } = await import('@/stores/menu')
  const userStore = useUserStore()
  const menuStore = useMenuStore()
  if (!userStore.fetched) {
    try {
      const info = await userStore.fetchInfo()
      menuStore.setMenus(info.menus)
      menuStore.buildRoutes(router)
      // 显式回放 path+query（fullPath 含查询串时作为 path 会被丢弃）
      return { path: to.path, query: to.query, replace: true }
    } catch {
      return '/login'
    }
  }
  // 未匹配任何路由（含刷新时序问题）兜底到工作台
  if (to.matched.length === 0) {
    return '/dashboard'
  }
  return true
})

router.afterEach(to => {
  document.title = to.meta?.title ? `${to.meta.title} · 天工 ERP` : '天工 ERP'
})

export default router
