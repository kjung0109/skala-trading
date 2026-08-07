import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { X } from 'lucide-react'
import { ApiError } from '../../api/client'
import { orderApi } from '../../api/endpoints'
import type { Order } from '../../api/types'
import { cn } from '../../lib/cn'
import { num, timeOf } from '../../lib/format'
import { qk } from '../../lib/queryClient'
import { useUIStore } from '../../store/useUIStore'
import { Panel } from '../ui/Panel'

/** 아직 체결되지 않은 주문. 여기서 바로 취소할 수 있다. */
export function OpenOrdersPanel() {
  const qc = useQueryClient()
  const toast = useUIStore((s) => s.toast)

  const { data } = useQuery({ queryKey: qk.myOrders, queryFn: orderApi.myOrders })
  const open = (data?.list ?? []).filter(
    (o) => o.status === 'OPEN' || o.status === 'PARTIALLY_FILLED',
  )

  const cancel = useMutation({
    mutationFn: (orderId: number) => orderApi.cancel(orderId),
    onSuccess: () => {
      toast('success', '주문을 취소했습니다')
      qc.invalidateQueries({ queryKey: qk.myOrders })
      qc.invalidateQueries({ queryKey: qk.me })
      qc.invalidateQueries({ queryKey: ['orderBook'] })
      qc.invalidateQueries({ queryKey: qk.myAudit })
    },
    onError: (e) => toast('error', e instanceof ApiError ? e.message : '취소에 실패했습니다'),
  })

  return (
    <Panel
      title="미체결 주문"
      action={
        <span className="text-[11px] font-bold text-foreground-tertiary tnum">
          {open.length}건
        </span>
      }
      className="h-[188px] shrink-0"
      scroll
    >
      <table className="w-full text-[12px] tnum">
        <thead className="sticky top-0 bg-surface-subtle text-[10px] font-bold text-foreground-tertiary">
          <tr>
            <Th className="pl-4 text-left">시각</Th>
            <Th className="text-left">종목</Th>
            <Th>구분</Th>
            <Th className="text-right">가격</Th>
            <Th className="text-right">수량</Th>
            <Th className="text-right">미체결</Th>
            <Th className="pr-3" />
          </tr>
        </thead>
        <tbody>
          {open.map((o) => (
            <tr key={o.orderId} className="border-b border-stroke-subtle last:border-0">
              <td className="py-2 pl-4 font-semibold text-foreground-tertiary">
                {timeOf(o.createdAt)}
              </td>
              <td className="font-bold text-foreground">{o.stockName}</td>
              <td className="text-center">
                <SideBadge order={o} />
              </td>
              <td className="text-right font-bold text-foreground">{num(o.price)}</td>
              <td className="text-right font-semibold text-foreground-secondary">
                {num(o.quantity)}
              </td>
              <td className="text-right font-bold text-primary">{num(o.remainingQuantity)}</td>
              <td className="pr-3 text-right">
                <button
                  onClick={() => cancel.mutate(o.orderId)}
                  disabled={cancel.isPending}
                  className="rounded-md p-1 text-foreground-disabled transition hover:bg-surface-muted hover:text-danger"
                  title="주문 취소"
                >
                  <X size={13} />
                </button>
              </td>
            </tr>
          ))}
          {open.length === 0 && (
            <tr>
              <td colSpan={7} className="py-12 text-center text-[12px] text-foreground-disabled">
                미체결 주문이 없습니다
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </Panel>
  )
}

function SideBadge({ order }: { order: Order }) {
  const buy = order.side === 'BUY'
  return (
    <span
      className={cn(
        'inline-block rounded-md border px-1.5 py-0.5 text-[10px] font-bold',
        buy ? 'border-up-border bg-up-bg text-up' : 'border-down-border bg-down-bg text-down',
      )}
    >
      {order.type === 'MARKET' ? '시장가 ' : ''}
      {buy ? '매수' : '매도'}
    </span>
  )
}

function Th({ children, className }: { children?: React.ReactNode; className?: string }) {
  return <th className={cn('py-1.5 text-center font-bold', className)}>{children}</th>
}
