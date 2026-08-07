import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useRef } from 'react'
import type { MarketEvent } from '../api/types'
import { qk } from '../lib/queryClient'
import { useMarketStream } from './useMarketStream'

/**
 * SSE로 들어온 시장 이벤트를 react-query 캐시 무효화로 연결한다.
 *
 * 봇이 초당 여러 건을 체결시키므로 이벤트마다 곧바로 refetch하면 요청이 폭주한다.
 * 어떤 쿼리를 다시 불러야 하는지만 모아뒀다가 300ms에 한 번씩 비운다.
 */
export function useMarketSync() {
  const qc = useQueryClient()
  const pending = useRef({ stocks: false, stockIds: new Set<number>(), account: false })
  const timer = useRef<number | null>(null)

  const flush = useCallback(() => {
    timer.current = null
    const { stocks, stockIds, account } = pending.current

    if (stocks) qc.invalidateQueries({ queryKey: qk.stocks })
    stockIds.forEach((id) => {
      qc.invalidateQueries({ queryKey: qk.orderBook(id) })
      qc.invalidateQueries({ queryKey: qk.trades(id) })
    })
    if (account) {
      qc.invalidateQueries({ queryKey: qk.me })
      qc.invalidateQueries({ queryKey: qk.myOrders })
      qc.invalidateQueries({ queryKey: qk.myTrades })
    }

    pending.current = { stocks: false, stockIds: new Set(), account: false }
  }, [qc])

  const onEvent = useCallback(
    (event: MarketEvent) => {
      pending.current.stockIds.add(event.stockId)
      if (event.type === 'TRADE') {
        // 체결이 나면 현재가와 내 잔고·주문 상태가 함께 바뀔 수 있다.
        pending.current.stocks = true
        pending.current.account = true
      }
      if (timer.current === null) timer.current = window.setTimeout(flush, 300)
    },
    [flush],
  )

  return useMarketStream(onEvent)
}
