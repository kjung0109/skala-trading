import Card from '../common/components/Card'
import { num, rate, signed, toneOf, won } from '../common/utils/format'
import type { AccountSummary } from '../common/api/types'

export default function AssetPage({ account }: { account: AccountSummary }) {
  return (
    <div className="space-y-3">
      <Card>
        <div className="px-5 py-5">
          <p className="text-sub text-ink-sub">총 자산</p>
          <p className="text-jumbo font-bold tnum mt-1">{won(account.totalAssets)}</p>
          <p className={`text-md font-semibold tnum mt-1 ${toneOf(account.totalProfitLoss)}`}>
            {signed(account.totalProfitLoss)}원 ({rate(account.totalReturnRate)})
          </p>

          <div className="grid grid-cols-2 gap-2 mt-5">
            <div className="rounded-btn bg-surface px-4 py-3">
              <p className="text-caption text-ink-hint">예수금</p>
              <p className="text-md font-bold tnum mt-0.5">{num(account.balance)}</p>
            </div>
            <div className="rounded-btn bg-surface px-4 py-3">
              <p className="text-caption text-ink-hint">평가 금액</p>
              <p className="text-md font-bold tnum mt-0.5">{num(account.totalValuation)}</p>
            </div>
          </div>
        </div>
      </Card>

      <Card title="보유 종목" right={<span className="text-caption text-ink-hint">{account.holdings.length}종목</span>}>
        {account.holdings.length === 0 ? (
          <p className="px-5 py-10 text-center text-sub text-ink-hint">보유 중인 종목이 없습니다</p>
        ) : (
          <ul className="pb-1">
            {account.holdings.map((h) => (
              <li
                key={h.stockId}
                className="px-5 py-3 border-b border-divider last:border-0 flex items-center"
              >
                <div className="flex-1 min-w-0">
                  <p className="text-md font-semibold truncate">{h.stockName}</p>
                  <p className="text-caption text-ink-hint tnum">
                    {num(h.quantity)}주 · 평단 {num(h.averagePrice)}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-md font-bold tnum">{num(h.valuation)}</p>
                  <p className={`text-caption tnum ${toneOf(h.profitLoss)}`}>
                    {signed(h.profitLoss)} ({rate(h.profitLossRate)})
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
