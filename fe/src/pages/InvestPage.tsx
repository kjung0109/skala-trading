import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { stockApi } from '../api/endpoints'
import { ChartPanel } from '../components/invest/ChartPanel'
import { OpenOrdersPanel } from '../components/invest/OpenOrdersPanel'
import { OrderBookPanel } from '../components/invest/OrderBookPanel'
import { OrderPanel } from '../components/invest/OrderPanel'
import { StockListPanel } from '../components/invest/StockListPanel'
import { TradeTapePanel } from '../components/invest/TradeTapePanel'
import { qk } from '../lib/queryClient'
import { useUIStore } from '../store/useUIStore'

/**
 * 주문 화면. 종목 · 차트 · 호가 · 주문을 한 화면에 둔다.
 * 주문을 넣으려고 화면을 옮겨 다닐 필요가 없어야 한다.
 */
export function InvestPage() {
  const { data } = useQuery({ queryKey: qk.stocks, queryFn: stockApi.list })
  const selectedStockId = useUIStore((s) => s.selectedStockId)
  const selectStock = useUIStore((s) => s.selectStock)

  const stocks = data?.list ?? []
  const stock = stocks.find((s) => s.id === selectedStockId)
  const firstStockId = stocks[0]?.id

  // 처음 들어오면 첫 종목을 자동으로 띄운다. 빈 화면을 보여주지 않는다.
  useEffect(() => {
    if (!stock && firstStockId !== undefined) selectStock(firstStockId)
  }, [stock, firstStockId, selectStock])

  return (
    <div className="flex h-full min-h-0 gap-2.5 p-2.5">
      <StockListPanel />

      {stock ? (
        <>
          <div className="flex min-w-0 flex-1 flex-col gap-2.5">
            <ChartPanel stock={stock} />
            <OpenOrdersPanel />
          </div>

          <div className="flex w-[300px] shrink-0 flex-col gap-2.5">
            <OrderBookPanel stockId={stock.id} />
            <TradeTapePanel stockId={stock.id} />
          </div>

          <OrderPanel stock={stock} />
        </>
      ) : (
        <div className="flex flex-1 items-center justify-center rounded-xl border border-stroke bg-surface text-[13px] text-foreground-disabled">
          종목을 불러오는 중입니다…
        </div>
      )}
    </div>
  )
}
