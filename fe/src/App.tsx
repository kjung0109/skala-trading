import { useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import { AppHeader } from './components/layout/AppHeader'
import { Sidebar } from './components/layout/Sidebar'
import { Toaster } from './components/ui/Toaster'
import { useMarketSync } from './hooks/useMarketSync'
import { LoginPage } from './pages/LoginPage'
import { useAuthStore } from './store/useAuthStore'

export function App() {
  const account = useAuthStore((s) => s.account)
  const initializing = useAuthStore((s) => s.initializing)
  const refresh = useAuthStore((s) => s.refresh)

  // 저장된 토큰이 아직 유효한지 먼저 확인한다.
  useEffect(() => {
    refresh()
  }, [refresh])

  // SSE는 로그인 여부와 무관하게 붙는다. 시세는 공개 데이터다.
  const { connected } = useMarketSync()

  if (initializing) return <Splash />
  if (!account) return <LoginPage />

  return (
    <>
      <AppHeader connected={connected} />
      <div className="flex min-h-0 flex-1">
        <Sidebar />
        <main className="min-w-0 flex-1 overflow-hidden">
          <Outlet />
        </main>
      </div>
      <Toaster />
    </>
  )
}

function Splash() {
  return (
    <div className="flex h-screen items-center justify-center">
      <span className="flex h-10 w-10 animate-pulse-dot items-center justify-center rounded-xl bg-primary text-[17px] font-black text-white">
        S
      </span>
    </div>
  )
}
