import { useQuery } from '@tanstack/react-query'
import { LogOut, Moon, Sun } from 'lucide-react'
import { accountApi } from '../../api/endpoints'
import { cn } from '../../lib/cn'
import { num, toneOf } from '../../lib/format'
import { qk } from '../../lib/queryClient'
import { useAuthStore } from '../../store/useAuthStore'
import { useUIStore } from '../../store/useUIStore'
import { LiveDot } from '../ui/LiveDot'
import { PriceChange } from '../ui/PriceChange'

type Props = { connected: boolean }

export function AppHeader({ connected }: Props) {
  const logout = useAuthStore((s) => s.logout)
  // 로그인 시점에 받아둔 값. 첫 조회가 끝나기 전 잠깐 쓰인다.
  const cached = useAuthStore((s) => s.account)

  // 예수금과 총자산은 체결될 때마다 바뀐다.
  // 스토어에 담아두면 로그인 시점 값에서 멈춰 있으므로, 다른 화면과 같은 캐시를 구독한다.
  // 주문이 체결되거나 시세가 움직이면 qk.me가 무효화되어 여기까지 함께 갱신된다.
  const { data } = useQuery({ queryKey: qk.me, queryFn: accountApi.me, enabled: cached != null })
  const account = data ?? cached

  return (
    <header className="flex h-header shrink-0 items-center gap-3 border-b border-stroke bg-surface px-4 xl:gap-4 xl:px-5">
      <div className="flex items-center gap-2.5">
        <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary text-[13px] font-black text-white">
          S
        </span>
        <span className="hidden text-[15px] font-extrabold tracking-tight text-foreground sm:inline">
          SKALA Trading
        </span>
      </div>

      {/* 시세가 멈춘 건지 연결이 끊긴 건지 구분되어야 한다. */}
      <span
        className={cn(
          'flex items-center gap-1.5 rounded-full border border-stroke px-2.5 py-1 text-[11px] font-bold',
          connected ? 'bg-surface-subtle text-live' : 'bg-surface-muted text-foreground-disabled',
        )}
      >
        <LiveDot active={connected} />
        {connected ? '실시간' : '연결 끊김'}
      </span>

      <div className="flex-1" />

      {account && (
        <>
          <div className="hidden items-center gap-5 lg:flex">
            <Metric label="예수금" value={`${num(account.balance)}원`} />
            <Metric label="총 자산" value={`${num(account.totalAssets)}원`} />
            <Metric
              label="총 손익"
              value={`${account.totalProfitLoss > 0 ? '+' : ''}${num(account.totalProfitLoss)}원`}
              tone={toneOf(account.totalProfitLoss)}
              badge={<PriceChange value={account.totalReturnRate} variant="badge" />}
            />
          </div>

          <div className="hidden h-6 w-px bg-stroke lg:block" />
        </>
      )}

      <div className="flex items-center gap-1">
        <ThemeToggle />
        {account && (
          <>
            <span className="ml-1 text-[13px] font-bold text-foreground">{account.accountId}</span>
            <IconButton label="로그아웃" onClick={logout}>
              <LogOut size={16} />
            </IconButton>
          </>
        )}
      </div>
    </header>
  )
}

/**
 * 라이트/다크 전환.
 * 색은 전부 CSS 변수라 data-theme만 바꾸면 화면 전체가 따라온다.
 */
function ThemeToggle() {
  const theme = useUIStore((s) => s.theme)
  const setTheme = useUIStore((s) => s.setTheme)

  return (
    <IconButton
      label={theme === 'dark' ? '라이트 모드로' : '다크 모드로'}
      onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
    >
      {theme === 'dark' ? <Moon size={16} /> : <Sun size={16} />}
    </IconButton>
  )
}

function IconButton({
  children,
  label,
  onClick,
}: {
  children: React.ReactNode
  label: string
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      title={label}
      aria-label={label}
      className="flex h-8 w-8 items-center justify-center rounded-lg text-foreground-tertiary transition hover:bg-surface-muted hover:text-foreground"
    >
      {children}
    </button>
  )
}

function Metric({
  label,
  value,
  tone,
  badge,
}: {
  label: string
  value: string
  tone?: string
  badge?: React.ReactNode
}) {
  return (
    <div className="flex items-center gap-1.5">
      <span className="text-[11px] font-semibold text-foreground-tertiary">{label}</span>
      <span className={cn('text-[13px] font-bold tnum', tone ?? 'text-foreground')}>{value}</span>
      {badge}
    </div>
  )
}
