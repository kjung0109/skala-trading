import { useQuery } from '@tanstack/react-query'
import { stockApi } from '../../api/endpoints'
import type { OrderBook, Stock } from '../../api/types'
import { cn } from '../../lib/cn'
import { num, toneOf } from '../../lib/format'
import { qk } from '../../lib/queryClient'
import { PriceChange } from '../ui/PriceChange'
import { StockAvatar } from '../ui/StockAvatar'
import { ChartAttribution, PriceChart } from './PriceChart'
import { StockSearch } from './StockSearch'

type Props = { stock: Stock }

/** 주문 화면 왼쪽. 종목 선택 · 현재가 · 요약 지표 · 가격 추이를 한 덩어리로 둔다. */
export function StockOverview({ stock }: Props) {
  const { data: trades } = useQuery({
    queryKey: qk.trades(stock.id),
    queryFn: () => stockApi.trades(stock.id),
  })
  const { data: book } = useQuery({
    queryKey: qk.orderBook(stock.id),
    queryFn: () => stockApi.orderBook(stock.id),
  })

  return (
    <section className="flex min-w-0 flex-1 flex-col overflow-hidden border-stroke bg-surface xl:basis-0 xl:border-r">
      <div className="shrink-0 border-b border-stroke px-3.5 py-2.5">
        <StockSearch stock={stock} />
      </div>

      <div className="flex shrink-0 items-center gap-2.5 border-b border-stroke px-3.5 py-2.5">
        <StockAvatar code={stock.code} name={stock.name} size="lg" />
        <div className="min-w-0 flex-1">
          <p className="truncate text-[14px] leading-tight font-extrabold text-foreground">
            {stock.name}
          </p>
          <p className="mt-0.5 text-[10px] font-medium text-foreground-disabled tnum">
            {stock.code} · KRX
          </p>
        </div>
        <div className="shrink-0 text-right">
          <p
            className={cn(
              'text-[22px] leading-none font-black tracking-tight tnum',
              toneOf(stock.change),
            )}
          >
            {num(stock.currentPrice)}
          </p>
          <div className="mt-1 flex items-center justify-end gap-1.5">
            <PriceChange value={stock.change} mode="amount" arrow className="text-[11px]" />
            <PriceChange value={stock.changeRate} variant="badge" />
          </div>
        </div>
      </div>

      <Summary stock={stock} book={book} tradeCount={trades?.length ?? 0} />

      <div className="flex min-h-[220px] flex-1 flex-col px-1 pt-1">
        <div className="min-h-0 flex-1">
          <PriceChart trades={trades ?? []} baseline={stock.previousPrice} />
        </div>
        <div className="flex shrink-0 justify-end px-2 pb-1">
          <ChartAttribution />
        </div>
      </div>
    </section>
  )
}

/**
 * 요약 지표.
 * 외부 시세 API를 쓰지 않으므로 시가총액·PER 같은 값은 없다.
 * 대신 이 시스템이 실제로 알고 있는 것 — 호가와 체결에서 나온 값만 보여준다.
 */
function Summary({
  stock,
  book,
  tradeCount,
}: {
  stock: Stock
  book?: OrderBook
  tradeCount: number
}) {
  const bestAsk = book?.askLevels[0]?.price
  const bestBid = book?.bidLevels[0]?.price
  const askQty = book?.askLevels.reduce((s, l) => s + l.quantity, 0) ?? 0
  const bidQty = book?.bidLevels.reduce((s, l) => s + l.quantity, 0) ?? 0

  return (
    <dl className="grid shrink-0 grid-cols-3 gap-x-3 gap-y-1.5 border-b border-stroke px-3.5 py-2.5 xl:grid-cols-6">
      <Item label="전일 종가" value={num(stock.previousPrice)} />
      <Item label="최우선 매도" value={bestAsk ? num(bestAsk) : '–'} tone="text-down" />
      <Item label="최우선 매수" value={bestBid ? num(bestBid) : '–'} tone="text-up" />
      <Item
        label="스프레드"
        value={bestAsk && bestBid ? num(bestAsk - bestBid) : '–'}
      />
      <Item label="호가 잔량" value={`${num(askQty)} / ${num(bidQty)}`} />
      <Item label="체결 건수" value={num(tradeCount)} />
    </dl>
  )
}

function Item({ label, value, tone }: { label: string; value: string; tone?: string }) {
  return (
    <div className="min-w-0">
      <dt className="truncate text-[10px] font-semibold text-foreground-disabled">{label}</dt>
      <dd className={cn('truncate text-[12px] font-bold tnum', tone ?? 'text-foreground')}>
        {value}
      </dd>
    </div>
  )
}
