import { useEffect, useRef, useState } from 'react'
import type { MarketEvent } from '../api/types'

/**
 * 백엔드의 SSE 스트림을 구독한다.
 *
 * EventSource는 연결이 끊기면 브라우저가 알아서 다시 붙는다.
 * 폴링과 달리 서버가 보낼 것이 있을 때만 데이터가 오므로 빈 요청이 오가지 않는다.
 */
export function useMarketStream(onEvent: (event: MarketEvent) => void) {
  const [connected, setConnected] = useState(false)
  // 콜백이 매 렌더마다 바뀌어도 구독을 다시 만들지 않도록 ref에 담아둔다.
  const handler = useRef(onEvent)
  handler.current = onEvent

  useEffect(() => {
    const source = new EventSource('/api/market/stream')

    source.addEventListener('connected', () => setConnected(true))
    source.onerror = () => setConnected(false)

    const relay = (e: MessageEvent) => {
      try {
        handler.current(JSON.parse(e.data) as MarketEvent)
        setConnected(true)
      } catch {
        /* 형식이 어긋난 이벤트는 무시한다 */
      }
    }

    source.addEventListener('trade', relay)
    source.addEventListener('order_book', relay)

    return () => source.close()
  }, [])

  return { connected }
}
