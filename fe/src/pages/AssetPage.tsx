import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { accountApi } from '../api/endpoints'
import { Panel } from '../components/ui/Panel'
import { StockAvatar } from '../components/ui/StockAvatar'
import { cn } from '../lib/cn'
import { badgeToneOf, num, rate, signed, toneOf, won } from '../lib/format'
import { qk } from '../lib/queryClient'
import { useUIStore } from '../store/useUIStore'

/** 내 자산. 예수금·평가액·손익과 보유 종목별 손익을 함께 본다. */
export function AssetPage() {
  const navigate = useNavigate()
  const selectStock = useUIStore((s) => s.selectStock)
  const { data: account } = useQuery({ queryKey: qk.me, queryFn: accountApi.me })

  if (!account) return null

  const holdings = account.holdings
  const cashRatio =
    account.totalAssets > 0 ? (account.balance / account.totalAssets) * 100 : 100

  return (
    <div className="flex h-full min-h-0 flex-col gap-2.5 p-2.5">
      {/* 요약 */}
      <section className="grid shrink-0 grid-cols-4 gap-2.5">
        <SummaryCard label="총 자산" value={won(account.totalAssets)} accent />
        <SummaryCard label="예수금" value={won(account.balance)} sub={`현금 비중 ${cashRatio.toFixed(1)}%`} />
        <SummaryCard label="주식 평가액" value={won(account.totalValuation)} sub={`매입 ${won(account.totalInvestment)}`} />
        <SummaryCard
          label="평가 손익"
          value={`${signed(account.totalProfitLoss)}원`}
          sub={rate(account.totalReturnRate)}
          tone={toneOf(account.totalProfitLoss)}
        />
      </section>

      <Panel
        title="보유 종목"
        action={
          <span className="text-[11px] font-bold text-foreground-tertiary tnum">
            {holdings.length}종목
          </span>
        }
        className="min-h-0 flex-1"
        scroll
      >
        <table className="w-full text-[13px] tnum">
          <thead className="sticky top-0 z-10 bg-surface-subtle text-[11px] font-bold text-foreground-tertiary">
            <tr className="border-b border-stroke">
              <th className="py-2.5 pl-5 text-left">종목</th>
              <th className="py-2.5 text-right">보유 수량</th>
              <th className="py-2.5 text-right">평균 단가</th>
              <th className="py-2.5 text-right">현재가</th>
              <th className="py-2.5 text-right">평가 금액</th>
              <th className="py-2.5 text-right">평가 손익</th>
              <th className="py-2.5 pr-5 text-right">수익률</th>
            </tr>
          </thead>
          <tbody>
            {holdings.map((h) => (
              <tr
                key={h.stockId}
                onClick={() => {
                  selectStock(h.stockId)
                  navigate('/invest')
                }}
                className="cursor-pointer border-b border-stroke-subtle transition last:border-0 hover:bg-surface-subtle"
              >
                <td className="py-2.5 pl-5">
                  <div className="flex items-center gap-2.5">
                    <StockAvatar code={h.stockCode} name={h.stockName} size="sm" />
                    <div>
                      <p className="font-bold text-foreground">{h.stockName}</p>
                      <p className="text-[11px] font-medium text-foreground-disabled">
                        {h.stockCode}
                      </p>
                    </div>
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
                <td className={cn('text-right font-bold', toneOf(h.profitLoss))}>
                  {signed(h.profitLoss)}
                </td>
                <td className="pr-5 text-right">
                  <span
                    className={cn(
                      'inline-block rounded-md border px-1.5 py-0.5 text-[12px] font-bold',
                      badgeToneOf(h.profitLoss),
                    )}
                  >
                    {rate(h.profitLossRate)}
                  </span>
                </td>
              </tr>
            ))}
            {holdings.length === 0 && (
              <tr>
                <td colSpan={7} className="py-16 text-center text-[13px] text-foreground-disabled">
                  보유 중인 종목이 없습니다
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}

function SummaryCard({
  label,
  value,
  sub,
  tone,
  accent,
}: {
  label: string
  value: string
  sub?: string
  tone?: string
  accent?: boolean
}) {
  return (
    <div
      className={cn(
        'rounded-xl border px-5 py-4',
        accent ? 'border-primary-border bg-primary-light' : 'border-stroke bg-surface',
      )}
    >
      <p
        className={cn(
          'text-[12px] font-bold',
          accent ? 'text-primary' : 'text-foreground-tertiary',
        )}
      >
        {label}
      </p>
      <p className={cn('mt-1.5 text-[22px] font-extrabold tnum', tone ?? 'text-foreground')}>
        {value}
      </p>
      {sub && (
        <p className={cn('mt-0.5 text-[12px] font-bold tnum', tone ?? 'text-foreground-tertiary')}>
          {sub}
        </p>
      )}
    </div>
  )
}
