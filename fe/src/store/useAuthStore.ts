import { create } from 'zustand'
import { tokenStore } from '../api/client'
import { accountApi } from '../api/endpoints'
import type { AccountSummary } from '../api/types'

type AuthState = {
  account: AccountSummary | null
  /** 최초 토큰 검증이 끝나기 전인지. 이 동안은 로그인 화면을 띄우지 않는다. */
  initializing: boolean
  pending: boolean
  login: (accountId: string, password: string) => Promise<void>
  signup: (accountId: string, password: string) => Promise<void>
  logout: () => void
  refresh: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set) => ({
  account: null,
  initializing: true,
  pending: false,

  refresh: async () => {
    if (!tokenStore.get()) {
      set({ account: null, initializing: false })
      return
    }
    try {
      set({ account: await accountApi.me(), initializing: false })
    } catch {
      // 토큰이 만료됐거나 유효하지 않다. 조용히 로그아웃시킨다.
      tokenStore.clear()
      set({ account: null, initializing: false })
    }
  },

  login: async (accountId, password) => {
    set({ pending: true })
    try {
      const result = await accountApi.login(accountId, password)
      tokenStore.set(result.accessToken)
      set({ account: await accountApi.me() })
    } finally {
      set({ pending: false })
    }
  },

  signup: async (accountId, password) => {
    set({ pending: true })
    try {
      await accountApi.signup(accountId, password)
      const result = await accountApi.login(accountId, password)
      tokenStore.set(result.accessToken)
      set({ account: await accountApi.me() })
    } finally {
      set({ pending: false })
    }
  },

  logout: () => {
    tokenStore.clear()
    set({ account: null })
  },
}))
