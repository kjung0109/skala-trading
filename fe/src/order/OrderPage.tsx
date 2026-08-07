import { useState } from 'react'
import Card from '../common/components/Card'
import Button from '../common/components/Button'
import OrderBook from './OrderBook'
import { num, rate, signed, toneOf, won } from '../common/utils/format'
import type { OrderBook as Book, OrderSide, OrderType, Stock } from '../common/api/types'

export default function OrderPage({
  stock,
  book,
  onSubmit,
  submitting,
}: {
  stock: Stock | null
  book: Book | null
  onSubmit: (params: { side: OrderSide; type: OrderType; price?: number; quantity: number }) => void
  submitting: boolean
}) {
  const [type, setType] = useState<OrderType>('LIMIT')
  const [price, setPrice] = useState<number | ''>('')
  const [quantity, setQuantity] = useState<number>(10)

  if (!stock) {
    return (
      <Card>
        <p className="px-5 py-12 text-center text-sub text-ink-hint">
          시세 탭에서 종목을 선택하세요
        </p>
      </Card>
    )
  }

  const effectivePrice = type === 'MARKET' ? stock.currentPrice : price === '' ? 0 : price
  const estimate = effectivePrice * quantity

  const submit = (side: OrderSide) =>
    onSubmit({
      side,
      type,
      price: type === 'LIMIT' ? (price === '' ? undefined : price) : undefined,
      quantity,
    })

  return (
    <div className="space-y-3">
      <Card>
        <div className="px-5 py-4 flex items-end justify-between">
          <div>
            <p className="text-card font-bold">{stock.name}</p>
            <p className="text-caption text-ink-hint tnum">{stock.code}</p>
          </div>
          <div className="text-right">
            <p className={`text-display font-bold tnum ${toneOf(stock.change)}`}>
              {num(stock.currentPrice)}
            </p>
            <p className={`text-sub tnum ${toneOf(stock.change)}`}>
              {signed(stock.change)} ({rate(stock.changeRate)})
            </p>
          </div>
        </div>
      </Card>

      <Card title="호가">
        <OrderBook
          book={book}
          onPickPrice={(p) => {
            setType('LIMIT')
            setPrice(p)
          }}
        />
      </Card>

      <Card title="주문">
        <div className="px-5 pb-5 pt-1 space-y-4">
          <div className="flex gap-2">
            {(['LIMIT', 'MARKET'] as OrderType[]).map((t) => (
              <button
                key={t}
                onClick={() => setType(t)}
                className={`flex-1 h-10 rounded-btn text-body font-semibold border
                  ${
                    type === t
                      ? 'border-primary text-primary bg-primary-tint'
                      : 'border-line text-ink-sub bg-white'
                  }`}
              >
                {t === 'LIMIT' ? '지정가' : '시장가'}
              </button>
            ))}
          </div>

          {type === 'LIMIT' ? (
            <div>
              <label className="block text-caption text-ink-hint mb-1.5">주문 가격</label>
              <input
                type="number"
                value={price}
                placeholder={String(stock.currentPrice)}
                onChange={(e) => setPrice(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full h-12 rounded-btn border border-line px-4 text-md text-right tnum outline-none focus:border-primary"
              />
              <p className="text-caption text-ink-hint mt-1.5">
                호가를 누르면 가격이 채워집니다
              </p>
            </div>
          ) : (
            <div className="rounded-btn bg-surface px-4 py-3">
              <p className="text-sub text-ink-sub">
                현재 걸려 있는 호가로 즉시 체결합니다.
                <br />
                물량이 모자라면 채운 만큼만 체결되고 나머지는 취소됩니다.
              </p>
            </div>
          )}

          <div>
            <label className="block text-caption text-ink-hint mb-1.5">수량</label>
            <input
              type="number"
              min={1}
              value={quantity}
              onChange={(e) => setQuantity(Math.max(1, Number(e.target.value)))}
              className="w-full h-12 rounded-btn border border-line px-4 text-md text-right tnum outline-none focus:border-primary"
            />
            <div className="flex gap-1.5 mt-2">
              {[1, 5, 10, 50].map((q) => (
                <button
                  key={q}
                  onClick={() => setQuantity(q)}
                  className="flex-1 h-8 rounded-icon border border-line text-caption text-ink-sub bg-white"
                >
                  {q}주
                </button>
              ))}
            </div>
          </div>

          <div className="rounded-btn bg-surface px-4 py-3 flex justify-between items-center">
            <span className="text-sub text-ink-sub">
              {type === 'MARKET' ? '예상 금액 (현재가 기준)' : '주문 금액'}
            </span>
            <span className="text-md font-bold tnum">{won(estimate)}</span>
          </div>

          <div className="flex gap-2">
            <Button variant="up" onClick={() => submit('BUY')} disabled={submitting}>
              매수
            </Button>
            <Button variant="down" onClick={() => submit('SELL')} disabled={submitting}>
              매도
            </Button>
          </div>
        </div>
      </Card>
    </div>
  )
}
