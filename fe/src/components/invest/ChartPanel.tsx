import { useQuery } from '@tanstack/react-query'
import { stockApi } from '../../api/endpoints'
import type { Stock } from '../../api/types'
import { cn } from '../../lib/cn'
import { arrowOf, badgeToneOf, num, rate, signed, toneOf } from '../../lib/format'
import { qk } from '../../lib/queryClient'
import { Panel } from '../ui/Panel'
import { StockAvatar } from '../ui/StockAvatar'
import { PriceChart } from './PriceChart'

type Props = { stock: Stock }

export function ChartPanel({ stock }: Props) {
  const { data: trades } = useQuery({
    queryKey: qk.trades(stock.id),
    queryFn: () => stockApi.trades(stock.id),
  })

  return (
    <Panel className="min-w-0 flex-1">
      <div className="flex h-full flex-col">
        <header className="flex shrink-0 items-start gap-3 border-b border-stroke-subtle px-5 py-4">
          <StockAvatar code={stock.code} name={stock.name} size="lg" />

          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h1 className="truncate text-[17px] font-extrabold text-foreground">{stock.name}</h1>
              <span className="rounded-md bg-surface-muted px-1.5 py-0.5 text-[11px] font-bold text-foreground-tertiary tnum">
                {stock.code}
              </span>
            </div>
            <div className="mt-1 flex items-baseline gap-2.5 tnum">
              <span className={cn('text-[28px] leading-none font-extrabold', toneOf(stock.change))}>
                {num(stock.currentPrice)}
              </span>
              <span className={cn('text-[13px] font-bold', toneOf(stock.change))}>
                {arrowOf(stock.change)} {signed(stock.change)}
              </span>
              <span
                className={cn(
                  'rounded-md border px-1.5 py-0.5 text-[12px] font-bold',
                  badgeToneOf(stock.change),
                )}
              >
                {rate(stock.changeRate)}
              </span>
            </div>
          </div>

          <div className="flex-1" />

          <dl className="flex gap-6 pt-1 text-right tnum">
            <Stat label="전일 종가" value={num(stock.previousPrice)} />
            <Stat label="체결 건수" value={num(trades?.length ?? 0)} />
          </dl>
        </header>

        <div className="min-h-0 flex-1 px-2 py-2">
          <PriceChart trades={trades ?? []} baseline={stock.previousPrice} />
        </div>
      </div>
    </Panel>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-[11px] font-semibold text-foreground-tertiary">{label}</dt>
      <dd className="mt-0.5 text-[14px] font-bold text-foreground">{value}</dd>
    </div>
  )
}
