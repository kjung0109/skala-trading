import Card from '../common/components/Card'
import { num, rate, signed, toneOf } from '../common/utils/format'
import type { Stock } from '../common/api/types'

export default function MarketPage({
  stocks,
  flashed,
  selectedId,
  onSelect,
}: {
  stocks: Stock[]
  flashed: Record<number, 'up' | 'down'>
  selectedId: number | null
  onSelect: (stock: Stock) => void
}) {
  return (
    <Card title="시세" right={<span className="text-caption text-ink-hint">실시간</span>}>
      <ul className="pb-1">
        {stocks.map((stock) => {
          const flash = flashed[stock.id]
          return (
            <li key={stock.id}>
              <button
                onClick={() => onSelect(stock)}
                className={`w-full flex items-center px-5 py-3 border-b border-divider last:border-0 text-left
                  ${selectedId === stock.id ? 'bg-primary-tint/60' : ''}
                  ${flash === 'up' ? 'flash-up' : flash === 'down' ? 'flash-down' : ''}`}
              >
                <div className="flex-1 min-w-0">
                  <p className="text-md font-semibold truncate">{stock.name}</p>
                  <p className="text-caption text-ink-hint tnum">{stock.code}</p>
                </div>
                <div className="text-right">
                  <p className={`text-md font-bold tnum ${toneOf(stock.change)}`}>
                    {num(stock.currentPrice)}
                  </p>
                  <p className={`text-caption tnum ${toneOf(stock.change)}`}>
                    {signed(stock.change)} ({rate(stock.changeRate)})
                  </p>
                </div>
              </button>
            </li>
          )
        })}
      </ul>
    </Card>
  )
}
