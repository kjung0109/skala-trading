import { useQuery } from '@tanstack/react-query'
import { accountApi } from '../../../api/endpoints'
import { num } from '../../../lib/format'
import { qk } from '../../../lib/queryClient'
import { PriceChange } from '../../ui/PriceChange'
import { StockAvatar } from '../../ui/StockAvatar'
import { PanelBody, PanelEmpty, Th } from './PanelTable'

/** 보유 종목. 주문 화면을 떠나지 않고 내 잔고를 확인할 수 있어야 한다. */
export function HoldingTable() {
  const { data: account } = useQuery({ queryKey: qk.me, queryFn: accountApi.me })

  const rows = account?.holdings ?? []
  if (rows.length === 0) return <PanelEmpty message="보유 중인 종목이 없습니다" />

  return (
    <PanelBody>
      <table className="w-full text-[11px] tnum">
        <thead className="sticky top-0 z-10 bg-surface-subtle text-foreground-disabled">
          <tr className="border-b border-stroke-subtle">
            <Th align="left">종목</Th>
            <Th align="right">보유</Th>
            <Th align="right">평균 단가</Th>
            <Th align="right">현재가</Th>
            <Th align="right">평가 금액</Th>
            <Th align="right">평가 손익</Th>
            <Th align="right">수익률</Th>
          </tr>
        </thead>
        <tbody>
          {rows.map((h) => (
            <tr key={h.stockId} className="border-b border-stroke-subtle last:border-0">
              <td className="py-1 pl-3">
                <div className="flex items-center gap-1.5">
                  <StockAvatar code={h.stockCode} name={h.stockName} size="sm" />
                  <span className="font-bold text-foreground">{h.stockName}</span>
                </div>
              </td>
              <td className="text-right font-semibold text-foreground-secondary">
                {num(h.quantity)}
              </td>
              <td className="text-right font-semibold text-foreground-secondary">
                {num(h.averagePrice)}
              </td>
              <td className="text-right font-bold text-foreground">{num(h.currentPrice)}</td>
              <td className="text-right font-bold text-foreground">{num(h.valuation)}</td>
              <td className="text-right">
                <PriceChange value={h.profitLoss} mode="amount" className="text-[11px]" />
              </td>
              <td className="pr-3 text-right">
                <PriceChange value={h.profitLossRate} className="text-[11px]" />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </PanelBody>
  )
}
