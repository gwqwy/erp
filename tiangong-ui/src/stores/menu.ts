import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Router } from 'vue-router'
import type { MenuNode } from '@/api/auth'
import { viewModules } from '@/router/views'

export const useMenuStore = defineStore('menu', () => {
  const menus = ref<MenuNode[]>([])
  const routesBuilt = ref(false)

  /** 按后端菜单树（目录→菜单）把组件挂到 layout 下；顶层菜单（parent 0）同样挂载 */
  function buildRoutes(router: Router) {
    for (const dir of menus.value.filter(m => m.type === 1)) {
      for (const child of dir.children || []) {
        if (child.type !== 2 || !child.component) continue
        const component = viewModules[child.component]
        if (!component) {
          console.warn('[menu] 组件未注册:', child.component)
          continue
        }
        const routePath = `${dir.path}/${child.path}`.replace(/\/+/g, '/')
        router.addRoute('layout', {
          path: routePath,
          name: `menu-${child.id}`,
          component,
          meta: { title: child.name }
        })
      }
    }
    // 顶层菜单（无父目录，如 文件中心 / IM 消息）
    for (const top of menus.value.filter(m => m.type === 2 && m.component)) {
      const component = viewModules[top.component]
      if (!component) {
        console.warn('[menu] 组件未注册:', top.component)
        continue
      }
      const routePath = `/${top.path}`.replace(/\/+/g, '/')
      router.addRoute('layout', {
        path: routePath,
        name: `menu-${top.id}`,
        component,
        meta: { title: top.name }
      })
    }
    routesBuilt.value = true
  }

  function setMenus(tree: MenuNode[]) {
    menus.value = tree
  }

  function reset() {
    menus.value = []
    routesBuilt.value = false
  }

  return { menus, routesBuilt, setMenus, buildRoutes, reset }
})
