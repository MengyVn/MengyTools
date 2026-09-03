import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/store/user'

/**
 * 按钮权限指令：
 *   v-permission="'article:add'"                       单个权限
 *   v-permission="['article:add', 'article:edit']"    多个权限，命中任一即可
 * 超管 permissions 为 ['*'] 时全部放行。
 */
export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    check(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    check(el, binding)
  }
}

function check(el: HTMLElement, binding: DirectiveBinding) {
  const value = binding.value
  if (!value) return
  const required = Array.isArray(value) ? value : [value]
  const userStore = useUserStore()
  const owned = userStore.userInfo?.permissions || []
  // 超管放行
  const ok = owned.includes('*') || required.some(p => owned.includes(p))
  if (!ok && el.parentNode) {
    el.parentNode.removeChild(el)
  }
}
