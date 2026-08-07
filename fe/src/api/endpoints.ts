import { api } from './client'
import type {
  AccountSummary,
  AuditLog,
  LoginResult,
  Order,
  OrderBook,
  OrderResult,
  Paged,
  Stock,
  Trade,
} from './types'

/**
 * 화면에서 URL 문자열을 직접 다루지 않도록 한 곳에 모은다.
 * 경로가 바뀌어도 고칠 곳은 여기 하나다.
 */
export const accountApi = {
  login: (accountId: string, password: string) =>
    api.post<LoginResult>('/accounts/login', { accountId, password }),
  signup: (accountId: string, password: string) =>
    api.post<AccountSummary>('/accounts', { accountId, password }),
  me: () => api.get<AccountSummary>('/accounts/me'),
}

export const stockApi = {
  list: () => api.get<Paged<Stock>>('/stocks/list?offset=0&count=100'),
  orderBook: (stockId: number) => api.get<OrderBook>(`/stocks/${stockId}/orderbook`),
  trades: (stockId: number) => api.get<Trade[]>(`/stocks/${stockId}/trades`),
}

export type PlaceOrderInput = {
  stockId: number
  side: 'BUY' | 'SELL'
  type: 'LIMIT' | 'MARKET'
  price?: number
  quantity: number
}

export const auditApi = {
  myLogs: () => api.get<Paged<AuditLog>>('/audit/me?offset=0&count=50'),
}

export const orderApi = {
  place: (input: PlaceOrderInput) => api.post<OrderResult>('/orders', input),
  cancel: (orderId: number) => api.post<Order>(`/orders/${orderId}/cancel`),
  myOrders: () => api.get<Paged<Order>>('/orders/me?offset=0&count=50'),
  myTrades: () => api.get<Paged<Trade>>('/orders/me/trades?offset=0&count=50'),
}
