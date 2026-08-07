import { LogOut } from 'lucide-react'
import { cn } from '../../lib/cn'
import { num, rate, toneOf } from '../../lib/format'
import { useAuthStore } from '../../store/useAuthStore'

type Props = { connected: boolean }

export function AppHeader({ connected }: Props) {
  const account = useAuthStore((s) => s.account)
  const logout = useAuthStore((s) => s.logout)

  return (
    <header className="flex h-header shrink-0 items-center gap-4 border-b border-stroke bg-surface px-5">
      <div className="flex items-center gap-2.5">
        <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary text-[13px] font-black text-white">
          S
        </span>
        <span className="text-[15px] font-extrabold tracking-tight text-foreground">
          SKALA Trading
        </span>
      </div>

      {/* 실시간 연결 상태. 시세가 멈춘 건지 연결이 끊긴 건지 구분되어야 한다. */}
      <span
        className={cn(
          'flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-bold',
          connected
            ? 'border-stroke bg-surface-subtle text-live'
            : 'border-stroke bg-surface-muted text-foreground-disabled',
        )}
      >
        <span
          className={cn(
            'h-1.5 w-1.5 rounded-full',
            connected ? 'animate-pulse-dot bg-live' : 'bg-foreground-disabled',
          )}
        />
        {connected ? '실시간' : '연결 끊김'}
      </span>

      <div className="flex-1" />

      {account && (
        <>
          <div className="flex items-center gap-5 tnum">
            <Metric label="예수금" value={`${num(account.balance)}원`} />
            <Metric label="총 평가" value={`${num(account.totalAssets)}원`} />
            <Metric
              label="총 손익"
              value={`${account.totalProfitLoss > 0 ? '+' : ''}${num(account.totalProfitLoss)}원`}
              sub={rate(account.totalReturnRate)}
              tone={toneOf(account.totalProfitLoss)}
            />
          </div>

          <div className="h-6 w-px bg-stroke" />

          <div className="flex items-center gap-2">
            <span className="text-[13px] font-bold text-foreground">{account.accountId}</span>
            <button
              onClick={logout}
              className="rounded-lg p-1.5 text-foreground-tertiary transition hover:bg-surface-muted hover:text-foreground"
              title="로그아웃"
            >
              <LogOut size={16} />
            </button>
          </div>
        </>
      )}
    </header>
  )
}

function Metric({
  label,
  value,
  sub,
  tone,
}: {
  label: string
  value: string
  sub?: string
  tone?: string
}) {
  return (
    <div className="flex items-baseline gap-1.5">
      <span className="text-[11px] font-semibold text-foreground-tertiary">{label}</span>
      <span className={cn('text-[13px] font-bold', tone ?? 'text-foreground')}>{value}</span>
      {sub && <span className={cn('text-[11px] font-bold', tone)}>{sub}</span>}
    </div>
  )
}
