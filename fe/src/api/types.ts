/** 백엔드 공통 응답 형식 */
export type ApiResponse<T> = {
  result: 'success' | 'fail'
  body?: T
  error?: { code: string; message: string }
}

export type Paged<T> = {
  total: number
  offset: number
  count: number
  hasNext: boolean
  list: T[]
}

export type Stock = {
  id: number
  code: string
  name: string
  currentPrice: number
  previousPrice: number
  change: number
  changeRate: number
}

export type OrderSide = 'BUY' | 'SELL'
export type OrderType = 'LIMIT' | 'MARKET'
export type OrderStatus = 'OPEN' | 'PARTIALLY_FILLED' | 'FILLED' | 'CANCELLED' | 'EXPIRED'

export type OrderBookLevel = { price: number; quantity: number }

export type OrderBook = {
  stockId: number
  stockCode: string
  stockName: string
  currentPrice: number
  askLevels: OrderBookLevel[]
  bidLevels: OrderBookLevel[]
}

export type Order = {
  orderId: number
  stockCode: string
  stockName: string
  side: OrderSide
  type: OrderType
  /** 시장가 주문은 가격을 부르지 않으므로 null이다. */
  price: number | null
  quantity: number
  filledQuantity: number
  remainingQuantity: number
  status: OrderStatus
  createdAt: string
}

export type Trade = {
  tradeId: number
  stockCode: string
  stockName: string
  price: number
  quantity: number
  amount: number
  mySide: OrderSide | null
  tradedAt: string
}

export type Holding = {
  stockId: number
  stockCode: string
  stockName: string
  quantity: number
  averagePrice: number
  currentPrice: number
  valuation: number
  profitLoss: number
  profitLossRate: number
}

export type AccountSummary = {
  accountId: string
  balance: number
  totalValuation: number
  totalAssets: number
  totalInvestment: number
  totalProfitLoss: number
  totalReturnRate: number
  holdings: Holding[]
}

export type LoginResult = {
  accountId: string
  balance: number
  accessToken: string
}

export type OrderResult = {
  order: Order
  tradedQuantity: number
  tradedAmount: number
  trades: Trade[]
  message: string
}

/** 캔들 하나. 서버가 체결을 시간 구간으로 접어 만들어 내려준다. */
export type Candle = {
  time: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

/** AOP가 남긴 주문 처리 기록 */
export type AuditLog = {
  logId: number
  action: 'PLACE' | 'CANCEL'
  detail: string
  success: boolean
  message: string
  elapsedMs: number
  createdAt: string
}

/** 실시간 시세 스트림 이벤트 */
export type MarketEvent = {
  type: 'TRADE' | 'ORDER_BOOK'
  stockId: number
  stockCode: string
  stockName: string
  price: number
  quantity: number | null
  side: OrderSide | null
  at: string
}
