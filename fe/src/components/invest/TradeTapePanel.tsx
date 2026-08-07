import { useQuery } from '@tanstack/react-query'
import { stockApi } from '../../api/endpoints'
import { cn } from '../../lib/cn'
import { num, timeOf } from '../../lib/format'
import { qk } from '../../lib/queryClient'
import { Panel } from '../ui/Panel'

type Props = { stockId: number }

/**
 * 체결 테이프.
 *
 * 체결이 매수·매도 중 어느 쪽이 걷어간 것인지는 공개 데이터에 없으므로,
 * 직전 체결가 대비 상승/하락(업틱·다운틱)으로 색을 준다. 실제 HTS와 같은 방식이다.
 */
export function TradeTapePanel({ stockId }: Props) {
  const { data: trades } = useQuery({
    queryKey: qk.trades(stockId),
    queryFn: () => stockApi.trades(stockId),
  })

  const rows = trades ?? []

  return (
    <Panel title="체결" className="h-[236px] shrink-0" scroll>
      <div className="sticky top-0 grid grid-cols-[1fr_1fr_auto] gap-2 border-b border-stroke-subtle bg-surface-subtle px-3 py-1.5 text-[10px] font-bold text-foreground-tertiary">
        <span>시각</span>
        <span className="text-right">체결가</span>
        <span className="w-12 text-right">수량</span>
      </div>

      <ul>
        {rows.map((t, i) => {
          // 목록이 최신순이므로 다음 항목이 직전 체결이다.
          const prev = rows[i + 1]?.price
          const tick = prev === undefined ? 0 : t.price - prev
          return (
            <li
              key={t.tradeId}
              className="grid animate-row-in grid-cols-[1fr_1fr_auto] gap-2 px-3 py-[5px] text-[11px] font-bold tnum odd:bg-surface-subtle/60"
            >
              <span className="text-foreground-tertiary">{timeOf(t.tradedAt)}</span>
              <span
                className={cn(
                  'text-right',
                  tick > 0 ? 'text-up' : tick < 0 ? 'text-down' : 'text-foreground',
                )}
              >
                {num(t.price)}
              </span>
              <span className="w-12 text-right text-foreground-secondary">{num(t.quantity)}</span>
            </li>
          )
        })}
        {rows.length === 0 && (
          <li className="py-10 text-center text-[12px] text-foreground-disabled">
            체결 내역이 없습니다
          </li>
        )}
      </ul>
    </Panel>
  )
}
