import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * v-perm 按钮级权限指令：权限不匹配时移除元素。
 * 用法：v-perm="'bas:product:create'"
 */
export const perm: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    if (!useUserStore().hasPermission(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}
