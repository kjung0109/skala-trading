import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, api, tokenStore } from './common/api/client'
import type {
  MarketEvent,
  Order,
  OrderBook as Book,
  OrderResult,
  OrderSide,
  OrderType,
  Paged,
  Stock,
  Trade,
} from './common/api/types'
import { useMarketStream } from './common/hooks/useMarketStream'
import BottomNav, { type TabKey } from './common/components/BottomNav'
import Toast, { type ToastState } from './common/components/Toast'
import { num, rate, toneOf } from './common/utils/format'
import { useAuth } from './auth/useAuth'
import LoginSheet from './auth/LoginSheet'
import MarketPage from './market/MarketPage'
import OrderPage from './order/OrderPage'
import AssetPage from './asset/AssetPage'
import HistoryPage from './history/HistoryPage'

export default function App() {
  const { account, loading, login, logout, refresh } = useAuth()
  const [tab, setTab] = useState<TabKey>('market')

  const [stocks, setStocks] = useState<Stock[]>([])
  const [selected, setSelected] = useState<Stock | null>(null)
  const [book, setBook] = useState<Book | null>(null)
  const [orders, setOrders] = useState<Order[]>([])
  const [trades, setTrades] = useState<Trade[]>([])

  const [flashed, setFlashed] = useState<Record<number, 'up' | 'down'>>({})
  const [toast, setToast] = useState<ToastState>(null)
  const [submitting, setSubmitting] = useState(false)

  const selectedRef = useRef<Stock | null>(null)
  selectedRef.current = selected

  /* ---------------- 조회 ---------------- */

  const loadStocks = useCallback(async () => {
    const paged = await api.get<Paged<Stock>>('/stocks/list?offset=0&count=30')
    setStocks(paged.list)
    // 선택 중인 종목의 현재가도 함께 갱신한다.
    setSelected((prev) => (prev ? paged.list.find((s) => s.id === prev.id) ?? prev : prev))
  }, [])

  const loadBook = useCallback(async (stockId: number) => {
    setBook(await api.get<Book>(`/stocks/${stockId}/orderbook`))
  }, [])

  const loadHistory = useCallback(async () => {
    if (!tokenStore.get()) return
    const [o, t] = await Promise.all([
      api.get<Paged<Order>>('/orders/me?offset=0&count=30'),
      api.get<Paged<Trade>>('/orders/me/trades?offset=0&count=30'),
    ])
    setOrders(o.list)
    setTrades(t.list)
  }, [])

  useEffect(() => {
    loadStocks()
    refresh()
  }, [loadStocks, refresh])

  useEffect(() => {
    if (selected) loadBook(selected.id)
  }, [selected?.id, loadBook])

  /* ---------------- 실시간 ---------------- */

  const onMarketEvent = useCallback(
    (event: MarketEvent) => {
      // 체결이 일어난 종목만 깜빡이게 한다.
      if (event.type === 'TRADE') {
        setStocks((prev) => {
          const before = prev.find((s) => s.id === event.stockId)
          if (before) {
            const dir = event.price >= before.currentPrice ? 'up' : 'down'
            setFlashed((f) => ({ ...f, [event.stockId]: dir }))
            setTimeout(() => setFlashed((f) => ({ ...f, [event.stockId]: undefined as never })), 600)
          }
          return prev
        })
      }

      // 시세는 이벤트가 올 때만 다시 읽는다. 주기적 폴링을 하지 않는다.
      loadStocks()
      if (selectedRef.current?.id === event.stockId) loadBook(event.stockId)
    },
    [loadStocks, loadBook],
  )

  const { connected } = useMarketStream(onMarketEvent)

  /* ---------------- 주문 ---------------- */

  const submitOrder = async (params: {
    side: OrderSide
    type: OrderType
    price?: number
    quantity: number
  }) => {
    if (!selected) return
    setSubmitting(true)
    try {
      const result = await api.post<OrderResult>('/orders', {
        stockId: selected.id,
        side: params.side,
        type: params.type,
        price: params.price,
        quantity: params.quantity,
      })
      setToast({
        tone: 'ok',
        title: params.side === 'BUY' ? '매수 주문 접수' : '매도 주문 접수',
        description: result.message,
      })
      await Promise.all([refresh(), loadStocks(), loadBook(selected.id), loadHistory()])
    } catch (e) {
      // 서버가 내려준 메시지를 그대로 보여준다.
      setToast({
        tone: 'error',
        title: '주문 실패',
        description: e instanceof ApiError ? e.message : '요청에 실패했습니다',
      })
    } finally {
      setSubmitting(false)
    }
  }

  const cancelOrder = async (orderId: number) => {
    try {
      await api.post(`/orders/${orderId}/cancel`)
      setToast({ tone: 'ok', title: '주문 취소', description: '미체결 잔량이 반환되었습니다' })
      await Promise.all([refresh(), loadHistory(), selected ? loadBook(selected.id) : Promise.resolve()])
    } catch (e) {
      setToast({
        tone: 'error',
        title: '취소 실패',
        description: e instanceof ApiError ? e.message : '요청에 실패했습니다',
      })
    }
  }

  useEffect(() => {
    if (tab === 'history') loadHistory()
  }, [tab, loadHistory])

  /* ---------------- 화면 ---------------- */

  const index = stocks[0]

  return (
    <div className="min-h-screen flex justify-center">
      <div className="w-full max-w-[430px] bg-page min-h-screen flex flex-col shadow-[var(--shadow-float)]">
        {/* AppBar */}
        <header className="sticky top-0 z-30 bg-white border-b border-line">
          <div className="h-14 px-5 flex items-center gap-2">
            <span className="w-6 h-6 rounded-icon bg-primary" />
            <h1 className="text-card font-bold flex-1">SKALA Trading</h1>
            <span className="flex items-center gap-1.5 text-caption text-ink-hint">
              <span
                className={`w-1.5 h-1.5 rounded-full ${connected ? 'bg-success' : 'bg-disabled'}`}
              />
              {connected ? '실시간' : '연결 중'}
            </span>
          </div>

          {account && (
            <div className="px-5 pb-3 flex items-center gap-2">
              <span className="text-sub font-semibold">{account.accountId}</span>
              <span className="text-sub text-ink-sub tnum flex-1">
                예수금 {num(account.balance)}원
              </span>
              {index && (
                <span className={`text-caption tnum ${toneOf(index.change)}`}>
                  {index.name} {rate(index.changeRate)}
                </span>
              )}
              <button onClick={logout} className="text-caption text-ink-hint underline">
                로그아웃
              </button>
            </div>
          )}
        </header>

        <main className="flex-1 p-3 pb-6">
          {tab === 'market' && (
            <MarketPage
              stocks={stocks}
              flashed={flashed}
              selectedId={selected?.id ?? null}
              onSelect={(s) => {
                setSelected(s)
                setTab('order')
              }}
            />
          )}
          {tab === 'order' && (
            <OrderPage stock={selected} book={book} onSubmit={submitOrder} submitting={submitting} />
          )}
          {tab === 'asset' &&
            (account ? (
              <AssetPage account={account} />
            ) : (
              <p className="py-20 text-center text-sub text-ink-hint">로그인이 필요합니다</p>
            ))}
          {tab === 'history' && (
            <HistoryPage orders={orders} trades={trades} onCancel={cancelOrder} />
          )}
        </main>

        <BottomNav current={tab} onChange={setTab} />
      </div>

      {!account && <LoginSheet onLogin={login} loading={loading} />}
      <Toast state={toast} onClose={() => setToast(null)} />
    </div>
  )
}
