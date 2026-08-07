import { useQuery } from '@tanstack/react-query'
import type { OrderBook, OrderBookLevel } from '../../api/types'
import { stockApi } from '../../api/endpoints'
import { cn } from '../../lib/cn'
import { num } from '../../lib/format'
import { qk } from '../../lib/queryClient'
import { useUIStore } from '../../store/useUIStore'
import { Panel } from '../ui/Panel'

type Props = { stockId: number }

/**
 * HTS 호가창. 가운데 가격을 두고 왼쪽에 매도 잔량, 오른쪽에 매수 잔량을 둔다.
 * 위쪽이 높은 가격이므로 매도는 내림차순으로 뒤집어 그린다.
 */
export function OrderBookPanel({ stockId }: Props) {
  const { data: book } = useQuery({
    queryKey: qk.orderBook(stockId),
    queryFn: () => stockApi.orderBook(stockId),
  })

  const pickPrice = useUIStore((s) => s.pickPrice)

  const asks = [...(book?.askLevels ?? [])].reverse()
  const bids = book?.bidLevels ?? []
  // 잔량 막대는 화면에 보이는 호가 중 최대값을 기준으로 그린다.
  const maxQty = Math.max(1, ...asks.map((l) => l.quantity), ...bids.map((l) => l.quantity))

  return (
    <Panel
      title="호가"
      action={<Spread book={book} />}
      className="min-h-0 flex-1"
      scroll
    >
      <div className="grid grid-cols-3 border-b border-stroke-subtle bg-surface-subtle py-1.5 text-[10px] font-bold text-foreground-tertiary">
        <span className="pl-3">매도잔량</span>
        <span className="text-center">호가</span>
        <span className="pr-3 text-right">매수잔량</span>
      </div>

      <div>
        {asks.map((level) => (
          <Row
            key={`a${level.price}`}
            level={level}
            side="ask"
            maxQty={maxQty}
            onClick={() => pickPrice(level.price)}
          />
        ))}

        <div className="flex items-center justify-center gap-2 border-y border-stroke bg-surface-muted py-1.5">
          <span className="text-[10px] font-bold text-foreground-tertiary">현재가</span>
          <span className="text-[14px] font-extrabold text-foreground tnum">
            {book ? num(book.currentPrice) : '–'}
          </span>
        </div>

        {bids.map((level) => (
          <Row
            key={`b${level.price}`}
            level={level}
            side="bid"
            maxQty={maxQty}
            onClick={() => pickPrice(level.price)}
          />
        ))}

        {asks.length === 0 && bids.length === 0 && (
          <p className="py-10 text-center text-[12px] text-foreground-disabled">
            대기 중인 주문이 없습니다
          </p>
        )}
      </div>
    </Panel>
  )
}

function Row({
  level,
  side,
  maxQty,
  onClick,
}: {
  level: OrderBookLevel
  side: 'ask' | 'bid'
  maxQty: number
  onClick: () => void
}) {
  const ratio = `${Math.max(4, (level.quantity / maxQty) * 100)}%`
  const isAsk = side === 'ask'

  return (
    <button
      onClick={onClick}
      className="grid w-full grid-cols-3 items-center py-[5px] text-[12px] font-bold transition hover:bg-surface-subtle tnum"
      title="클릭하면 주문 가격에 채워집니다"
    >
      {/* 매도 잔량 — 오른쪽 끝(호가 쪽)에서 왼쪽으로 자란다 */}
      <span className="relative flex h-5 items-center justify-end pr-2 pl-3">
        {isAsk && (
          <span
            className="absolute inset-y-0 right-0 rounded-l-sm bg-down-fill"
            style={{ width: ratio }}
          />
        )}
        <span className="relative text-foreground-secondary">
          {isAsk ? num(level.quantity) : ''}
        </span>
      </span>

      <span className={cn('text-center', isAsk ? 'text-down' : 'text-up')}>
        {num(level.price)}
      </span>

      <span className="relative flex h-5 items-center justify-start pr-3 pl-2">
        {!isAsk && (
          <span
            className="absolute inset-y-0 left-0 rounded-r-sm bg-up-fill"
            style={{ width: ratio }}
          />
        )}
        <span className="relative text-foreground-secondary">
          {!isAsk ? num(level.quantity) : ''}
        </span>
      </span>
    </button>
  )
}

/** 최우선 매도호가와 매수호가의 차이. 얼마나 빡빡한 시장인지 보여준다. */
function Spread({ book }: { book?: OrderBook }) {
  const bestAsk = book?.askLevels[0]?.price
  const bestBid = book?.bidLevels[0]?.price
  if (!bestAsk || !bestBid) return null

  return (
    <span className="text-[11px] font-bold text-foreground-tertiary tnum">
      스프레드 {num(bestAsk - bestBid)}
    </span>
  )
}
