import { num } from '../common/utils/format'
import type { OrderBook as Book } from '../common/api/types'

/**
 * 호가창.
 * 매도는 위, 매수는 아래에 두어 실제 HTS 배치와 맞춘다.
 * 잔량 막대 길이로 물량 분포가 한눈에 보이게 했다.
 */
export default function OrderBook({
  book,
  onPickPrice,
}: {
  book: Book | null
  onPickPrice: (price: number) => void
}) {
  if (!book) {
    return <div className="px-5 py-10 text-center text-sub text-ink-hint">종목을 선택하세요</div>
  }

  const maxQuantity = Math.max(
    1,
    ...book.askLevels.map((l) => l.quantity),
    ...book.bidLevels.map((l) => l.quantity),
  )

  const Row = ({
    price,
    quantity,
    side,
  }: {
    price: number
    quantity: number
    side: 'ask' | 'bid'
  }) => (
    <button
      onClick={() => onPickPrice(price)}
      className="w-full grid grid-cols-2 items-center h-9 px-5 hover:bg-surface"
      title="클릭하면 주문 가격으로 채워집니다"
    >
      {side === 'ask' ? (
        <>
          <span className="relative flex justify-end pr-3">
            <span
              className="absolute right-0 top-1/2 -translate-y-1/2 h-6 bg-down-bg rounded-sm"
              style={{ width: `${(quantity / maxQuantity) * 100}%` }}
            />
            <span className="relative text-sub tnum text-ink-sub">{num(quantity)}</span>
          </span>
          <span className="text-sub font-bold tnum text-down text-left pl-3">{num(price)}</span>
        </>
      ) : (
        <>
          <span className="text-sub font-bold tnum text-up text-right pr-3">{num(price)}</span>
          <span className="relative flex justify-start pl-3">
            <span
              className="absolute left-0 top-1/2 -translate-y-1/2 h-6 bg-up-bg rounded-sm"
              style={{ width: `${(quantity / maxQuantity) * 100}%` }}
            />
            <span className="relative text-sub tnum text-ink-sub">{num(quantity)}</span>
          </span>
        </>
      )}
    </button>
  )

  return (
    <div className="pb-2">
      <div className="grid grid-cols-2 px-5 py-2 text-caption text-ink-hint border-b border-divider">
        <span className="text-right pr-3">매도 잔량</span>
        <span className="text-left pl-3">가격 / 매수 잔량</span>
      </div>

      {book.askLevels.length === 0 && (
        <p className="py-3 text-center text-caption text-ink-hint">매도 호가 없음</p>
      )}
      {book.askLevels.map((l) => (
        <Row key={`a-${l.price}`} price={l.price} quantity={l.quantity} side="ask" />
      ))}

      <div className="my-1 mx-5 h-px bg-line" />

      {book.bidLevels.map((l) => (
        <Row key={`b-${l.price}`} price={l.price} quantity={l.quantity} side="bid" />
      ))}
      {book.bidLevels.length === 0 && (
        <p className="py-3 text-center text-caption text-ink-hint">매수 호가 없음</p>
      )}
    </div>
  )
}
