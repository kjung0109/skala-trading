import { useCallback, useState } from 'react'
import { api, tokenStore } from '../common/api/client'
import type { AccountSummary, LoginResult } from '../common/api/types'

export function useAuth() {
  const [account, setAccount] = useState<AccountSummary | null>(null)
  const [loading, setLoading] = useState(false)

  const refresh = useCallback(async () => {
    if (!tokenStore.get()) return null
    try {
      const summary = await api.get<AccountSummary>('/accounts/me')
      setAccount(summary)
      return summary
    } catch {
      // 토큰이 만료되었거나 유효하지 않으면 로그인 상태를 지운다.
      tokenStore.clear()
      setAccount(null)
      return null
    }
  }, [])

  const login = useCallback(
    async (accountId: string, password: string) => {
      setLoading(true)
      try {
        const result = await api.post<LoginResult>('/accounts/login', { accountId, password })
        tokenStore.set(result.accessToken)
        await refresh()
      } finally {
        setLoading(false)
      }
    },
    [refresh],
  )

  const logout = useCallback(() => {
    tokenStore.clear()
    setAccount(null)
  }, [])

  return { account, loading, login, logout, refresh }
}
