import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { stockApi } from '../../api/endpoints'
import type { OrderBookLevel, Stock } from '../../api/types'
import { cn } from '../../lib/cn'
import { num } from '../../lib/format'
import { qk } from '../../lib/queryClient'
import { useUIStore } from '../../store/useUIStore'
import { PriceChange } from '../ui/PriceChange'

type Props = { stock: Stock }

/**
 * 호가창.
 *
 * 위가 높은 가격이므로 매도호가는 내림차순으로 뒤집어 그리고, 그 아래 현재가,
 * 다시 그 아래 매수호가를 둔다. 잔량은 막대 길이로 함께 보여 어느 가격대가
 * 두꺼운지 숫자를 읽지 않고도 알 수 있게 한다.
 *
 * 가격을 누르면 주문 패널의 가격 입력란이 채워진다.
 */
export function OrderBookPanel({ stock }: Props) {
  const { data: book } = useQuery({
    queryKey: qk.orderBook(stock.id),
    queryFn: () => stockApi.orderBook(stock.id),
  })

  const pickPrice = useUIStore((s) => s.pickPrice)
  const [picked, setPicked] = useState<number | null>(null)

  const asks = [...(book?.askLevels ?? [])].reverse()
  const bids = book?.bidLevels ?? []
  const askTotal = asks.reduce((sum, l) => sum + l.quantity, 0)
  const bidTotal = bids.reduce((sum, l) => sum + l.quantity, 0)
  const maxQty = Math.max(1, ...asks.map((l) => l.quantity), ...bids.map((l) => l.quantity))

  const select = (price: number) => {
    setPicked(price)
    pickPrice(price)
  }

  return (
    <section className="flex h-[400px] shrink-0 flex-col overflow-hidden border-t border-stroke bg-surface xl:h-auto xl:w-[196px] xl:border-t-0 xl:border-r">
      <div className="flex h-9 shrink-0 items-center justify-between border-b border-stroke px-2.5">
        <span className="text-[12px] font-bold text-foreground">호가</span>
        <span className="text-[10px] text-foreground-disabled">잔량</span>
      </div>

      <TotalRow side="ask" total={askTotal} />

      <div className="min-h-0 flex-1 overflow-y-auto">
        {asks.map((level) => (
          <Row
            key={`a${level.price}`}
            level={level}
            side="ask"
            maxQty={maxQty}
            selected={picked === level.price}
            onClick={() => select(level.price)}
          />
        ))}

        <CurrentPriceRow stock={stock} />

        {bids.map((level) => (
          <Row
            key={`b${level.price}`}
            level={level}
            side="bid"
            maxQty={maxQty}
            selected={picked === level.price}
            onClick={() => select(level.price)}
          />
        ))}

        {asks.length === 0 && bids.length === 0 && (
          <p className="px-3 py-10 text-center text-[11px] leading-relaxed text-foreground-disabled">
            대기 중인 주문이 없습니다
          </p>
        )}
      </div>

      <TotalRow side="bid" total={bidTotal} />
    </section>
  )
}

/** 한쪽 호가 전체 잔량. 매수·매도 어느 쪽이 두꺼운지 한눈에 보인다. */
function TotalRow({ side, total }: { side: 'ask' | 'bid'; total: number }) {
  const isAsk = side === 'ask'
  return (
    <div
      className={cn(
        'flex shrink-0 items-center justify-between px-2.5 py-1',
        isAsk ? 'bg-down-bg' : 'border-t border-stroke bg-up-bg',
      )}
    >
      <span className={cn('text-[10px] font-semibold', isAsk ? 'text-down' : 'text-up')}>
        {isAsk ? '매도잔량' : '매수잔량'}
      </span>
      <span className={cn('text-[11px] font-bold tnum', isAsk ? 'text-down' : 'text-up')}>
        {num(total)}
      </span>
    </div>
  )
}

/** 현재가 행. 체결로 값이 바뀌면 잠깐 강조해 움직였다는 걸 보이게 한다. */
function CurrentPriceRow({ stock }: { stock: Stock }) {
  const previous = useRef(stock.currentPrice)
  const [flash, setFlash] = useState<'up' | 'down' | null>(null)

  useEffect(() => {
    const before = previous.current
    previous.current = stock.currentPrice
    if (stock.currentPrice === before) return

    setFlash(stock.currentPrice > before ? 'up' : 'down')
    const timer = setTimeout(() => setFlash(null), 700)
    return () => clearTimeout(timer)
  }, [stock.currentPrice])

  const up = stock.change > 0
  const down = stock.change < 0

  return (
    <div
      className={cn(
        'flex items-center justify-between border-y-2 px-2.5 py-1.5',
        up && 'border-up-border bg-up-bg',
        down && 'border-down-border bg-down-bg',
        !up && !down && 'border-stroke bg-surface-muted',
        flash === 'up' && 'flash-up',
        flash === 'down' && 'flash-down',
      )}
    >
      <span
        className={cn(
          'text-[15px] font-black tnum',
          up && 'text-up',
          down && 'text-down',
          !up && !down && 'text-foreground',
        )}
      >
        {num(stock.currentPrice)}
      </span>
      <PriceChange value={stock.changeRate} variant="badge" />
    </div>
  )
}

function Row({
  level,
  side,
  maxQty,
  selected,
  onClick,
}: {
  level: OrderBookLevel
  side: 'ask' | 'bid'
  maxQty: number
  selected: boolean
  onClick: () => void
}) {
  const isAsk = side === 'ask'
  const depth = `${Math.max(4, (level.quantity / maxQty) * 100)}%`

  return (
    <button
      onClick={onClick}
      title="클릭하면 주문 가격에 채워집니다"
      className={cn(
        'relative flex w-full items-center justify-between px-2.5 py-[3px] text-left transition-colors',
        selected
          ? isAsk
            ? 'bg-down-bg'
            : 'bg-up-bg'
          : isAsk
            ? 'hover:bg-down-bg/50'
            : 'hover:bg-up-bg/50',
      )}
    >
      {/* 잔량 막대는 오른쪽 끝에서 왼쪽으로 자란다 */}
      <span
        className={cn('absolute inset-y-0 right-0', isAsk ? 'bg-down-fill' : 'bg-up-fill')}
        style={{ width: depth }}
      />
      <span
        className={cn('relative text-[12px] font-bold tnum', isAsk ? 'text-down' : 'text-up')}
      >
        {num(level.price)}
      </span>
      <span className="relative text-[10px] font-semibold text-foreground-tertiary tnum">
        {num(level.quantity)}
      </span>
    </button>
  )
}
