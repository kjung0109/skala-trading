import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { auditApi, orderApi } from '../api/endpoints'
import type { AuditLog, OrderStatus } from '../api/types'
import { Panel } from '../components/ui/Panel'
import { cn } from '../lib/cn'
import { dateOf, num, timeOf, won } from '../lib/format'
import { qk } from '../lib/queryClient'

const STATUS_LABEL: Record<OrderStatus, string> = {
  OPEN: '대기',
  PARTIALLY_FILLED: '일부 체결',
  FILLED: '체결 완료',
  CANCELLED: '취소',
  EXPIRED: '미체결 소멸',
}

const STATUS_CLASS: Record<OrderStatus, string> = {
  OPEN: 'border-primary-border bg-primary-light text-primary',
  PARTIALLY_FILLED: 'border-primary-border bg-primary-light text-primary',
  FILLED: 'border-stroke bg-surface-muted text-foreground-secondary',
  CANCELLED: 'border-stroke bg-surface-muted text-foreground-disabled',
  EXPIRED: 'border-stroke bg-surface-muted text-foreground-disabled',
}

/** 주문과 체결을 나눠 본다. 주문은 냈지만 체결은 안 됐을 수 있기 때문이다. */
export function HistoryPage() {
  const [tab, setTab] = useState<'orders' | 'trades' | 'audit'>('orders')

  const orders = useQuery({ queryKey: qk.myOrders, queryFn: orderApi.myOrders })
  const trades = useQuery({ queryKey: qk.myTrades, queryFn: orderApi.myTrades })
  const audit = useQuery({ queryKey: qk.myAudit, queryFn: auditApi.myLogs })

  return (
    <div className="min-h-0 p-2.5 xl:h-full">
      <Panel
        title={
          <div className="flex gap-1 rounded-lg bg-surface-muted p-0.5">
            {(
              [
                ['orders', '주문 내역'],
                ['trades', '체결 내역'],
                ['audit', '처리 기록'],
              ] as const
            ).map(([key, label]) => (
              <button
                key={key}
                onClick={() => setTab(key)}
                className={cn(
                  'rounded-md px-3 py-1 text-[12px] font-bold transition',
                  tab === key
                    ? 'bg-surface text-primary shadow-control'
                    : 'text-foreground-tertiary hover:text-foreground',
                )}
              >
                {label}
              </button>
            ))}
          </div>
        }
        className="min-h-[420px] xl:h-full"
        scroll
      >
        {tab === 'audit' ? (
          <AuditTable logs={audit.data?.list ?? []} />
        ) : tab === 'orders' ? (
          <table className="w-full min-w-[760px] text-[13px] tnum">
            <Head cols={['주문 시각', '종목', '구분', '유형', '주문가', '주문 수량', '체결 수량', '상태']} />
            <tbody>
              {(orders.data?.list ?? []).map((o) => (
                <tr key={o.orderId} className="border-b border-stroke-subtle last:border-0">
                  <td className="py-2.5 pl-5 font-semibold text-foreground-tertiary">
                    <span className="mr-1.5 text-foreground-disabled">{dateOf(o.createdAt)}</span>
                    {timeOf(o.createdAt)}
                  </td>
                  <td className="font-bold text-foreground">{o.stockName}</td>
                  <td className="text-center">
                    <span className={cn('font-bold', o.side === 'BUY' ? 'text-up' : 'text-down')}>
                      {o.side === 'BUY' ? '매수' : '매도'}
                    </span>
                  </td>
                  <td className="text-center font-semibold text-foreground-tertiary">
                    {o.type === 'MARKET' ? '시장가' : '지정가'}
                  </td>
                  <td className="text-right font-bold text-foreground">{num(o.price)}</td>
                  <td className="text-right font-semibold text-foreground-secondary">
                    {num(o.quantity)}
                  </td>
                  <td className="text-right font-bold text-foreground">{num(o.filledQuantity)}</td>
                  <td className="pr-5 text-right">
                    <span
                      className={cn(
                        'inline-block rounded-md border px-1.5 py-0.5 text-[11px] font-bold',
                        STATUS_CLASS[o.status],
                      )}
                    >
                      {STATUS_LABEL[o.status]}
                    </span>
                  </td>
                </tr>
              ))}
              <Empty show={(orders.data?.list ?? []).length === 0} cols={8} text="주문 내역이 없습니다" />
            </tbody>
          </table>
        ) : (
          <table className="w-full min-w-[760px] text-[13px] tnum">
            <Head cols={['체결 시각', '종목', '구분', '체결가', '수량', '체결 금액']} />
            <tbody>
              {(trades.data?.list ?? []).map((t) => (
                <tr key={t.tradeId} className="border-b border-stroke-subtle last:border-0">
                  <td className="py-2.5 pl-5 font-semibold text-foreground-tertiary">
                    <span className="mr-1.5 text-foreground-disabled">{dateOf(t.tradedAt)}</span>
                    {timeOf(t.tradedAt)}
                  </td>
                  <td className="font-bold text-foreground">{t.stockName}</td>
                  <td className="text-center">
                    <span className={cn('font-bold', t.mySide === 'BUY' ? 'text-up' : 'text-down')}>
                      {t.mySide === 'BUY' ? '매수' : '매도'}
                    </span>
                  </td>
                  <td className="text-right font-bold text-foreground">{num(t.price)}</td>
                  <td className="text-right font-semibold text-foreground-secondary">
                    {num(t.quantity)}
                  </td>
                  <td className="pr-5 text-right font-bold text-foreground">{won(t.amount)}</td>
                </tr>
              ))}
              <Empty show={(trades.data?.list ?? []).length === 0} cols={6} text="체결 내역이 없습니다" />
            </tbody>
          </table>
        )}
      </Panel>
    </div>
  )
}

/**
 * AOP가 남긴 처리 기록.
 *
 * 거절된 주문은 롤백되어 주문 내역에는 남지 않는다.
 * 무엇을 시도했다가 왜 안 됐는지는 이 탭에서만 볼 수 있다.
 */
function AuditTable({ logs }: { logs: AuditLog[] }) {
  return (
    <table className="w-full min-w-[760px] text-[13px] tnum">
      <Head cols={['처리 시각', '동작', '요청 내용', '결과', '메시지', '소요']} />
      <tbody>
        {logs.map((l) => (
          <tr key={l.logId} className="border-b border-stroke-subtle last:border-0">
            <td className="py-2.5 pl-5 font-semibold text-foreground-tertiary">
              <span className="mr-1.5 text-foreground-disabled">{dateOf(l.createdAt)}</span>
              {timeOf(l.createdAt)}
            </td>
            <td className="text-center font-bold text-foreground-secondary">
              {l.action === 'PLACE' ? '주문' : '취소'}
            </td>
            <td className="text-center font-semibold text-foreground">{l.detail}</td>
            <td className="text-center">
              <span
                className={cn(
                  'inline-block rounded-md border px-1.5 py-0.5 text-[11px] font-bold',
                  l.success
                    ? 'border-stroke bg-surface-muted text-foreground-secondary'
                    : 'border-up-border bg-up-bg text-up',
                )}
              >
                {l.success ? '성공' : '거절'}
              </span>
            </td>
            <td
              className={cn(
                'text-center font-semibold',
                l.success ? 'text-foreground-tertiary' : 'text-up',
              )}
            >
              {l.message}
            </td>
            <td className="pr-5 text-right font-semibold text-foreground-tertiary">
              {num(l.elapsedMs)}ms
            </td>
          </tr>
        ))}
        <Empty show={logs.length === 0} cols={6} text="처리 기록이 없습니다" />
      </tbody>
    </table>
  )
}

function Head({ cols }: { cols: string[] }) {
  return (
    <thead className="sticky top-0 z-10 bg-surface-subtle text-[11px] font-bold text-foreground-tertiary">
      <tr className="border-b border-stroke">
        {cols.map((c, i) => (
          <th
            key={c}
            className={cn(
              'py-2.5',
              i === 0 ? 'pl-5 text-left' : i === cols.length - 1 ? 'pr-5 text-right' : 'text-center',
            )}
          >
            {c}
          </th>
        ))}
      </tr>
    </thead>
  )
}

function Empty({ show, cols, text }: { show: boolean; cols: number; text: string }) {
  if (!show) return null
  return (
    <tr>
      <td colSpan={cols} className="py-16 text-center text-[13px] text-foreground-disabled">
        {text}
      </td>
    </tr>
  )
}
