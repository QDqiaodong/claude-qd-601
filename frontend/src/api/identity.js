import { reactive, watch } from 'vue'

/**
 * 当前操作人身份（演示系统没有登录态，在页面顶部切换）。
 * 后端会对每个写操作再次校验角色：普通工作人员无法绕过前端完成负责人专属的复训放行。
 */
const STORAGE_KEY = 'eq-health-operator'

function restore() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') {
        return {
          name: typeof parsed.name === 'string' ? parsed.name : '',
          role: parsed.role === 'MANAGER' ? 'MANAGER' : 'STAFF'
        }
      }
    }
  } catch (error) {
    // localStorage 不可用时退回默认身份
  }
  return { name: '值班员 小冯', role: 'STAFF' }
}

export const operator = reactive(restore())

watch(
  operator,
  (value) => {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
    } catch (error) {
      // 忽略持久化失败
    }
  },
  { deep: true }
)

export function isManager() {
  return operator.role === 'MANAGER'
}

/**
 * 生成一次提交专用的幂等键：打开动作弹窗时生成，重复点击 / 重试保持不变；
 * 遇到并发冲突需要重新载入时，重新生成一个。
 */
export function newRequestKey() {
  const rand =
    typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `k-${Date.now()}-${Math.random().toString(16).slice(2)}`
  return `req-${rand}`.slice(0, 64)
}
