import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { stockApi } from '../api/endpoints'
import type { Stock } from '../api/types'
import { Panel } from '../components/ui/Panel'
import { PriceChange } from '../components/ui/PriceChange'
import { StockAvatar } from '../components/ui/StockAvatar'
import { cn } from '../lib/cn'
import { num, toneOf } from '../lib/format'
import { qk } from '../lib/queryClient'
import { useUIStore } from '../store/useUIStore'

type SortKey = 'name' | 'currentPrice' | 'changeRate'

/** 전체 종목 시세판. 등락률로 정렬해 오늘 시장이 어떤지 한눈에 본다. */
export function MarketPage() {
  const navigate = useNavigate()
  const selectStock = useUIStore((s) => s.selectStock)
  const [sort, setSort] = useState<{ key: SortKey; desc: boolean }>({
    key: 'changeRate',
    desc: true,
  })

  const { data } = useQuery({ queryKey: qk.stocks, queryFn: stockApi.list })

  const stocks = useMemo(() => {
    const list = [...(data?.list ?? [])]
    list.sort((a, b) => {
      const va = a[sort.key]
      const vb = b[sort.key]
      const cmp = typeof va === 'string' ? va.localeCompare(vb as string) : (va as number) - (vb as number)
      return sort.desc ? -cmp : cmp
    })
    return list
  }, [data, sort])

  const toggle = (key: SortKey) =>
    setSort((s) => (s.key === key ? { key, desc: !s.desc } : { key, desc: true }))

  const rising = stocks.filter((s) => s.change > 0).length
  const falling = stocks.filter((s) => s.change < 0).length

  const open = (stock: Stock) => {
    selectStock(stock.id)
    navigate('/invest')
  }

  return (
    <div className="min-h-0 p-2.5 xl:h-full">
      <Panel
        title="전체 시세"
        action={
          <span className="text-[11px] font-bold tnum">
            <span className="text-up">▲ {rising}</span>
            <span className="mx-1.5 text-stroke-input">|</span>
            <span className="text-down">▼ {falling}</span>
          </span>
        }
        className="min-h-[420px] xl:h-full"
        scroll
      >
        <table className="w-full min-w-[640px] text-[13px] tnum">
          <thead className="sticky top-0 z-10 bg-surface-subtle text-[11px] font-bold text-foreground-tertiary">
            <tr className="border-b border-stroke">
              <SortableTh className="w-[34%] pl-5 text-left" onClick={() => toggle('name')}>
                종목
              </SortableTh>
              <SortableTh className="text-right" onClick={() => toggle('currentPrice')}>
                현재가
              </SortableTh>
              <th className="py-2.5 pr-2 text-right">전일 대비</th>
              <SortableTh className="pr-5 text-right" onClick={() => toggle('changeRate')}>
                등락률
              </SortableTh>
              <th className="py-2.5 pr-5 text-right">전일 종가</th>
            </tr>
          </thead>
          <tbody>
            {stocks.map((stock) => (
              <tr
                key={stock.id}
                onClick={() => open(stock)}
                className="cursor-pointer border-b border-stroke-subtle transition last:border-0 hover:bg-surface-subtle"
              >
                <td className="py-2.5 pl-5">
                  <div className="flex items-center gap-2.5">
                    <StockAvatar code={stock.code} name={stock.name} size="sm" />
                    <div>
                      <p className="font-bold text-foreground">{stock.name}</p>
                      <p className="text-[11px] font-medium text-foreground-disabled">
                        {stock.code}
                      </p>
                    </div>
                  </div>
                </td>
                <td className={cn('text-right font-bold', toneOf(stock.change))}>
                  {num(stock.currentPrice)}
                </td>
                <td className="pr-2 text-right">
                  <PriceChange value={stock.change} mode="amount" arrow />
                </td>
                <td className="pr-5 text-right">
                  <PriceChange value={stock.changeRate} variant="badge" />
                </td>
                <td className="pr-5 text-right font-semibold text-foreground-tertiary">
                  {num(stock.previousPrice)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}

function SortableTh({
  children,
  className,
  onClick,
}: {
  children: React.ReactNode
  className?: string
  onClick: () => void
}) {
  return (
    <th className={cn('py-2.5', className)}>
      <button onClick={onClick} className="font-bold transition hover:text-primary">
        {children}
      </button>
    </th>
  )
}
