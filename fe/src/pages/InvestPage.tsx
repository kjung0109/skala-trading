import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { stockApi } from '../api/endpoints'
import { BottomPanels } from '../components/invest/BottomPanels'
import { OrderBookPanel } from '../components/invest/OrderBookPanel'
import { OrderPanel } from '../components/invest/OrderPanel'
import { StockOverview } from '../components/invest/StockOverview'
import { qk } from '../lib/queryClient'
import { useUIStore } from '../store/useUIStore'

/**
 * 주문 화면.
 *
 * 위쪽은 종목·차트 / 호가 / 주문을 나란히 두고, 아래쪽에 체결과 내 계좌 상태를 둔다.
 * 카드로 나누지 않고 경계선만으로 구분해 한 화면에 최대한 많은 정보를 담는다.
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

  if (!stock) {
    return (
      <div className="flex h-full items-center justify-center text-[13px] text-foreground-disabled">
        종목을 불러오는 중입니다…
      </div>
    )
  }

  return (
    <div className="flex min-h-0 flex-col bg-surface xl:h-full">
      <div className="flex min-h-0 flex-col xl:flex-1 xl:flex-row xl:overflow-hidden">
        <StockOverview stock={stock} />
        <OrderBookPanel stock={stock} />
        <OrderPanel stock={stock} />
      </div>

      <BottomPanels stockId={stock.id} />
    </div>
  )
}
