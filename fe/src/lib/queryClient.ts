import { QueryClient } from '@tanstack/react-query'

/**
 * 시세는 SSE로 밀려오므로 주기적 폴링(refetchInterval)은 쓰지 않는다.
 * 이벤트를 받은 쪽에서 invalidateQueries로 필요한 것만 다시 불러온다.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 3_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

export const qk = {
  stocks: ['stocks'] as const,
  orderBook: (stockId: number) => ['orderBook', stockId] as const,
  trades: (stockId: number) => ['trades', stockId] as const,
  chart: (stockId: number, interval: number) => ['chart', stockId, interval] as const,
  me: ['account', 'me'] as const,
  myOrders: ['orders', 'me'] as const,
  myTrades: ['trades', 'me'] as const,
  myAudit: ['audit', 'me'] as const,
}
