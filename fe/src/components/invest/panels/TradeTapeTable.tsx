import { useQuery } from '@tanstack/react-query'
import { stockApi } from '../../../api/endpoints'
import { cn } from '../../../lib/cn'
import { num, timeOf } from '../../../lib/format'
import { qk } from '../../../lib/queryClient'
import { PanelBody, PanelEmpty, Th } from './PanelTable'

type Props = { stockId: number }

/**
 * 실시간 체결.
 *
 * 어느 쪽이 걷어간 체결인지는 공개 데이터에 없다.
 * 직전 체결가 대비 상승·하락(업틱·다운틱)으로 색을 준다. 실제 HTS와 같은 방식이다.
 */
export function TradeTapeTable({ stockId }: Props) {
  const { data } = useQuery({
    queryKey: qk.trades(stockId),
    queryFn: () => stockApi.trades(stockId),
  })

  const rows = data ?? []
  if (rows.length === 0) return <PanelEmpty message="체결 내역이 없습니다" />

  return (
    <PanelBody>
      <table className="w-full text-[11px] tnum">
        <thead className="sticky top-0 z-10 bg-surface-subtle text-foreground-disabled">
          <tr className="border-b border-stroke-subtle">
            <Th align="left">체결 시각</Th>
            <Th align="right">체결가</Th>
            <Th align="right">수량</Th>
            <Th align="right">체결 금액</Th>
          </tr>
        </thead>
        <tbody>
          {rows.map((t, i) => {
            // 목록이 최신순이므로 다음 항목이 직전 체결이다.
            const prev = rows[i + 1]?.price
            const tick = prev === undefined ? 0 : t.price - prev
            return (
              <tr key={t.tradeId} className="border-b border-stroke-subtle last:border-0">
                <td className="py-[5px] pl-3 font-semibold text-foreground-tertiary">
                  {timeOf(t.tradedAt)}
                </td>
                <td
                  className={cn(
                    'py-[5px] pr-3 text-right font-bold',
                    tick > 0 ? 'text-up' : tick < 0 ? 'text-down' : 'text-foreground',
                  )}
                >
                  {num(t.price)}
                </td>
                <td className="py-[5px] pr-3 text-right font-semibold text-foreground-secondary">
                  {num(t.quantity)}
                </td>
                <td className="py-[5px] pr-3 text-right font-semibold text-foreground-tertiary">
                  {num(t.amount)}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </PanelBody>
  )
}
