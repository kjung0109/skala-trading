import { useQuery } from '@tanstack/react-query'
import { orderApi } from '../../../api/endpoints'
import { cn } from '../../../lib/cn'
import { num, timeOf } from '../../../lib/format'
import { qk } from '../../../lib/queryClient'
import { PanelBody, PanelEmpty, Th } from './PanelTable'

/** 내 체결 내역. 주문은 냈지만 체결은 안 됐을 수 있어 주문과 따로 본다. */
export function MyTradesTable() {
  const { data } = useQuery({ queryKey: qk.myTrades, queryFn: orderApi.myTrades })

  const rows = data?.list ?? []
  if (rows.length === 0) return <PanelEmpty message="체결된 주문이 없습니다" />

  return (
    <PanelBody>
      <table className="w-full text-[11px] tnum">
        <thead className="sticky top-0 z-10 bg-surface-subtle text-foreground-disabled">
          <tr className="border-b border-stroke-subtle">
            <Th align="left">체결 시각</Th>
            <Th align="left">종목</Th>
            <Th>구분</Th>
            <Th align="right">체결가</Th>
            <Th align="right">수량</Th>
            <Th align="right">체결 금액</Th>
          </tr>
        </thead>
        <tbody>
          {rows.map((t) => (
            <tr key={t.tradeId} className="border-b border-stroke-subtle last:border-0">
              <td className="py-[5px] pl-3 font-semibold text-foreground-tertiary">
                {timeOf(t.tradedAt)}
              </td>
              <td className="py-[5px] font-bold text-foreground">{t.stockName}</td>
              <td
                className={cn(
                  'py-[5px] text-center font-bold',
                  t.mySide === 'BUY' ? 'text-up' : 'text-down',
                )}
              >
                {t.mySide === 'BUY' ? '매수' : '매도'}
              </td>
              <td className="py-[5px] text-right font-bold text-foreground">{num(t.price)}</td>
              <td className="py-[5px] text-right font-semibold text-foreground-secondary">
                {num(t.quantity)}
              </td>
              <td className="py-[5px] pr-3 text-right font-bold text-foreground">
                {num(t.amount)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </PanelBody>
  )
}
