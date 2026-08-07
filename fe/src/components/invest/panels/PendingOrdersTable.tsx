import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { X } from 'lucide-react'
import { ApiError } from '../../../api/client'
import { orderApi } from '../../../api/endpoints'
import { cn } from '../../../lib/cn'
import { num, timeOf } from '../../../lib/format'
import { qk } from '../../../lib/queryClient'
import { useUIStore } from '../../../store/useUIStore'
import { PanelBody, PanelEmpty, Th } from './PanelTable'

/** 아직 체결되지 않은 주문. 여기서 바로 취소할 수 있다. */
export function PendingOrdersTable() {
  const qc = useQueryClient()
  const toast = useUIStore((s) => s.toast)

  const { data } = useQuery({ queryKey: qk.myOrders, queryFn: orderApi.myOrders })
  const rows = (data?.list ?? []).filter(
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

  if (rows.length === 0) {
    return <PanelEmpty message="미체결 주문이 없습니다" sub="지정가 주문이 남으면 여기에 표시됩니다" />
  }

  return (
    <PanelBody>
      <table className="w-full text-[11px] tnum">
        <thead className="sticky top-0 z-10 bg-surface-subtle text-foreground-disabled">
          <tr className="border-b border-stroke-subtle">
            <Th align="left">주문 시각</Th>
            <Th align="left">종목</Th>
            <Th>구분</Th>
            <Th align="right">주문가</Th>
            <Th align="right">수량</Th>
            <Th align="right">미체결</Th>
            <Th align="right" />
          </tr>
        </thead>
        <tbody>
          {rows.map((o) => (
            <tr key={o.orderId} className="border-b border-stroke-subtle last:border-0">
              <td className="py-[5px] pl-3 font-semibold text-foreground-tertiary">
                {timeOf(o.createdAt)}
              </td>
              <td className="py-[5px] font-bold text-foreground">{o.stockName}</td>
              <td className="py-[5px] text-center">
                <span
                  className={cn(
                    'inline-block rounded border px-1 py-px text-[10px] font-bold',
                    o.side === 'BUY'
                      ? 'border-up-border bg-up-bg text-up'
                      : 'border-down-border bg-down-bg text-down',
                  )}
                >
                  {o.type === 'MARKET' ? '시장가 ' : ''}
                  {o.side === 'BUY' ? '매수' : '매도'}
                </span>
              </td>
              <td className="py-[5px] text-right font-bold text-foreground">{num(o.price)}</td>
              <td className="py-[5px] text-right font-semibold text-foreground-secondary">
                {num(o.quantity)}
              </td>
              <td className="py-[5px] text-right font-bold text-primary">
                {num(o.remainingQuantity)}
              </td>
              <td className="py-[5px] pr-2 text-right">
                <button
                  onClick={() => cancel.mutate(o.orderId)}
                  disabled={cancel.isPending}
                  title="주문 취소"
                  className="rounded p-1 text-foreground-disabled transition hover:bg-surface-muted hover:text-danger"
                >
                  <X size={12} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </PanelBody>
  )
}
