import Card from '../common/components/Card'
import { num, timeOf, won } from '../common/utils/format'
import type { Order, Trade } from '../common/api/types'

const STATUS_LABEL: Record<Order['status'], string> = {
  OPEN: '미체결',
  PARTIALLY_FILLED: '부분체결',
  FILLED: '체결완료',
  CANCELLED: '취소',
  EXPIRED: '잔량취소',
}

const STATUS_TONE: Record<Order['status'], string> = {
  OPEN: 'bg-warning-bg text-warning',
  PARTIALLY_FILLED: 'bg-primary-tint text-primary',
  FILLED: 'bg-success-bg text-success',
  CANCELLED: 'bg-surface text-ink-hint',
  EXPIRED: 'bg-surface text-ink-hint',
}

export default function HistoryPage({
  orders,
  trades,
  onCancel,
}: {
  orders: Order[]
  trades: Trade[]
  onCancel: (orderId: number) => void
}) {
  return (
    <div className="space-y-3">
      <Card title="내 주문">
        {orders.length === 0 ? (
          <p className="px-5 py-10 text-center text-sub text-ink-hint">주문 내역이 없습니다</p>
        ) : (
          <ul className="pb-1">
            {orders.map((o) => (
              <li key={o.orderId} className="px-5 py-3 border-b border-divider last:border-0">
                <div className="flex items-center gap-2">
                  <span className={`text-caption font-bold ${o.side === 'BUY' ? 'text-up' : 'text-down'}`}>
                    {o.side === 'BUY' ? '매수' : '매도'}
                  </span>
                  <span className="text-md font-semibold flex-1 truncate">{o.stockName}</span>
                  <span className={`text-caption px-2 py-0.5 rounded-badge font-semibold ${STATUS_TONE[o.status]}`}>
                    {STATUS_LABEL[o.status]}
                  </span>
                </div>

                <div className="flex items-center justify-between mt-1">
                  <p className="text-caption text-ink-hint tnum">
                    {o.type === 'MARKET' ? '시장가' : `${num(o.price)}원`} · {num(o.quantity)}주
                    {o.filledQuantity > 0 && ` · 체결 ${num(o.filledQuantity)}주`}
                    {o.remainingQuantity > 0 && o.status !== 'EXPIRED' && ` · 잔량 ${num(o.remainingQuantity)}주`}
                  </p>

                  {(o.status === 'OPEN' || o.status === 'PARTIALLY_FILLED') && (
                    <button
                      onClick={() => onCancel(o.orderId)}
                      className="text-caption font-semibold text-ink-sub border border-line rounded-icon px-2.5 py-1 bg-white"
                    >
                      취소
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card title="체결 내역">
        {trades.length === 0 ? (
          <p className="px-5 py-10 text-center text-sub text-ink-hint">체결 내역이 없습니다</p>
        ) : (
          <ul className="pb-1">
            {trades.map((t) => (
              <li
                key={t.tradeId}
                className="px-5 py-3 border-b border-divider last:border-0 flex items-center"
              >
                <div className="flex-1 min-w-0">
                  <p className="text-md font-semibold truncate">
                    <span className={`mr-1.5 text-caption font-bold ${t.mySide === 'BUY' ? 'text-up' : 'text-down'}`}>
                      {t.mySide === 'BUY' ? '매수' : '매도'}
                    </span>
                    {t.stockName}
                  </p>
                  <p className="text-caption text-ink-hint tnum">
                    {timeOf(t.tradedAt)} · {num(t.price)}원 × {num(t.quantity)}주
                  </p>
                </div>
                <p className="text-md font-bold tnum">{won(t.amount)}</p>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
