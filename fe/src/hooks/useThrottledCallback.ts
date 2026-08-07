import { useCallback, useEffect, useRef } from 'react'

/**
 * 짧은 시간에 몰려 들어오는 호출을 묶어준다.
 *
 * 체결 이벤트는 초당 여러 건 올 수 있는데 그때마다 서버를 다시 조회하면
 * 요청이 과하게 늘어난다. 마지막 호출은 버리지 않고 지연 실행해
 * 최신 상태로 반드시 한 번 더 맞춘다.
 */
export function useThrottledCallback(fn: () => void, waitMs: number) {
  const target = useRef(fn)
  target.current = fn

  const lastRun = useRef(0)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => () => { if (timer.current) clearTimeout(timer.current) }, [])

  return useCallback(() => {
    const now = Date.now()
    const elapsed = now - lastRun.current

    if (elapsed >= waitMs) {
      lastRun.current = now
      target.current()
      return
    }

    if (!timer.current) {
      timer.current = setTimeout(() => {
        timer.current = null
        lastRun.current = Date.now()
        target.current()
      }, waitMs - elapsed)
    }
  }, [waitMs])
}
