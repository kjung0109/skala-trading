import { useQuery } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import { useMemo, useState } from 'react'
import { stockApi } from '../../api/endpoints'
import type { Stock } from '../../api/types'
import { cn } from '../../lib/cn'
import { arrowOf, num, rate, toneOf } from '../../lib/format'
import { qk } from '../../lib/queryClient'
import { useUIStore } from '../../store/useUIStore'
import { Panel } from '../ui/Panel'
import { StockAvatar } from '../ui/StockAvatar'

export function StockListPanel() {
  const [keyword, setKeyword] = useState('')
  const selectedStockId = useUIStore((s) => s.selectedStockId)
  const selectStock = useUIStore((s) => s.selectStock)

  const { data } = useQuery({ queryKey: qk.stocks, queryFn: stockApi.list })

  const stocks = useMemo(() => {
    const list = data?.list ?? []
    const k = keyword.trim().toLowerCase()
    if (!k) return list
    return list.filter(
      (s) => s.name.toLowerCase().includes(k) || s.code.includes(k),
    )
  }, [data, keyword])

  return (
    <Panel title="종목" className="w-[264px] shrink-0" scroll>
      <div className="sticky top-0 z-10 bg-surface px-3 pt-3 pb-2">
        <div className="flex items-center gap-2 rounded-lg border border-stroke-input bg-surface-subtle px-2.5 py-2 focus-within:border-primary focus-within:bg-surface">
          <Search size={14} className="shrink-0 text-foreground-disabled" />
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="종목명 · 코드 검색"
            className="w-full bg-transparent text-[12px] font-medium outline-none placeholder:text-foreground-disabled"
          />
        </div>
      </div>

      <ul className="px-2 pb-2">
        {stocks.map((stock) => (
          <StockListRow
            key={stock.id}
            stock={stock}
            active={stock.id === selectedStockId}
            onSelect={() => selectStock(stock.id)}
          />
        ))}
        {stocks.length === 0 && (
          <li className="px-2 py-8 text-center text-[12px] text-foreground-disabled">
            검색 결과가 없습니다
          </li>
        )}
      </ul>
    </Panel>
  )
}

function StockListRow({
  stock,
  active,
  onSelect,
}: {
  stock: Stock
  active: boolean
  onSelect: () => void
}) {
  return (
    <li>
      <button
        onClick={onSelect}
        className={cn(
          'flex w-full items-center gap-2.5 rounded-lg px-2 py-2.5 text-left transition',
          active ? 'bg-primary-light' : 'hover:bg-surface-subtle',
        )}
      >
        <StockAvatar code={stock.code} name={stock.name} size="sm" />
        <div className="min-w-0 flex-1">
          <p
            className={cn(
              'truncate text-[13px] font-bold',
              active ? 'text-primary' : 'text-foreground',
            )}
          >
            {stock.name}
          </p>
          <p className="text-[11px] font-medium text-foreground-disabled tnum">{stock.code}</p>
        </div>
        <div className="text-right tnum">
          <p className="text-[13px] font-bold text-foreground">{num(stock.currentPrice)}</p>
          <p className={cn('text-[11px] font-bold', toneOf(stock.change))}>
            {arrowOf(stock.change)} {rate(stock.changeRate)}
          </p>
        </div>
      </button>
    </li>
  )
}
