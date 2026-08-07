import { useQuery } from '@tanstack/react-query'
import { ChevronDown, Search } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { stockApi } from '../../api/endpoints'
import type { Stock } from '../../api/types'
import { cn } from '../../lib/cn'
import { num } from '../../lib/format'
import { qk } from '../../lib/queryClient'
import { useUIStore } from '../../store/useUIStore'
import { PriceChange } from '../ui/PriceChange'
import { StockAvatar } from '../ui/StockAvatar'

type Props = { stock: Stock }

/**
 * 종목 선택.
 *
 * 종목 목록을 화면에 항상 띄워 두면 주문 화면의 폭을 잡아먹는다.
 * 전체 목록은 시세 화면에 두고, 여기서는 검색으로 고른다.
 */
export function StockSearch({ stock }: Props) {
  const { data } = useQuery({ queryKey: qk.stocks, queryFn: stockApi.list })
  const selectStock = useUIStore((s) => s.selectStock)

  const [open, setOpen] = useState(false)
  const [keyword, setKeyword] = useState('')
  const boxRef = useRef<HTMLDivElement>(null)

  // 바깥을 클릭하면 닫는다.
  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => {
      if (!boxRef.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [open])

  const matches = useMemo(() => {
    const list = data?.list ?? []
    const k = keyword.trim().toLowerCase()
    if (!k) return list
    return list.filter((s) => s.name.toLowerCase().includes(k) || s.code.includes(k))
  }, [data, keyword])

  const choose = (id: number) => {
    selectStock(id)
    setKeyword('')
    setOpen(false)
  }

  return (
    <div ref={boxRef} className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className={cn(
          'flex w-full items-center gap-2 rounded-lg border bg-surface-subtle px-2.5 py-2 text-left transition',
          open ? 'border-primary bg-surface' : 'border-stroke-input hover:border-primary-border',
        )}
      >
        <Search size={14} className="shrink-0 text-foreground-disabled" />
        <span className="min-w-0 flex-1 truncate text-[12px] font-bold text-foreground">
          {stock.name}
          <span className="ml-1.5 font-semibold text-foreground-disabled tnum">{stock.code}</span>
        </span>
        <ChevronDown
          size={14}
          className={cn('shrink-0 text-foreground-disabled transition', open && 'rotate-180')}
        />
      </button>

      {open && (
        <div className="absolute top-full right-0 left-0 z-30 mt-1 overflow-hidden rounded-lg border border-stroke bg-surface shadow-dropdown">
          <div className="border-b border-stroke-subtle p-2">
            <input
              autoFocus
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="종목명 · 코드 검색"
              className="w-full rounded-md bg-surface-subtle px-2.5 py-1.5 text-[12px] font-medium outline-none placeholder:text-foreground-disabled"
            />
          </div>

          <ul className="max-h-[320px] overflow-y-auto py-1">
            {matches.map((s) => (
              <li key={s.id}>
                <button
                  onClick={() => choose(s.id)}
                  className={cn(
                    'flex w-full items-center gap-2 px-2.5 py-2 text-left transition',
                    s.id === stock.id ? 'bg-primary-light' : 'hover:bg-surface-subtle',
                  )}
                >
                  <StockAvatar code={s.code} name={s.name} size="sm" />
                  <div className="min-w-0 flex-1">
                    <p
                      className={cn(
                        'truncate text-[12px] font-bold',
                        s.id === stock.id ? 'text-primary' : 'text-foreground',
                      )}
                    >
                      {s.name}
                    </p>
                    <p className="text-[10px] font-medium text-foreground-disabled tnum">{s.code}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-[12px] font-bold text-foreground tnum">
                      {num(s.currentPrice)}
                    </p>
                    <PriceChange value={s.changeRate} className="text-[10px]" />
                  </div>
                </button>
              </li>
            ))}
            {matches.length === 0 && (
              <li className="px-3 py-6 text-center text-[12px] text-foreground-disabled">
                검색 결과가 없습니다
              </li>
            )}
          </ul>
        </div>
      )}
    </div>
  )
}
