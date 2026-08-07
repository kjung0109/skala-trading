import {
  AreaSeries,
  createChart,
  type IChartApi,
  type ISeriesApi,
  type UTCTimestamp,
} from 'lightweight-charts'
import { useEffect, useMemo, useRef } from 'react'
import type { Trade } from '../../api/types'

type Props = {
  trades: Trade[]
  /** 색 기준선. 전일 종가보다 위면 빨강, 아래면 파랑으로 그린다. */
  baseline: number
}

const UP = '#E8393E'
const DOWN = '#0075E8'

/**
 * 체결 내역을 이어 붙여 만든 가격 추이.
 *
 * 외부 시세 API를 쓰지 않으므로 차트도 우리 체결 데이터로만 그린다.
 * lightweight-charts는 같은 시각의 점을 허용하지 않아 초 단위로 접어서 넣는다.
 */
export function PriceChart({ trades, baseline }: Props) {
  const boxRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Area'> | null>(null)

  const points = useMemo(() => {
    const bySecond = new Map<number, number>()
    // 서버는 최신순으로 내려주므로 뒤집어 시간 오름차순으로 만든다.
    for (const t of [...trades].reverse()) {
      bySecond.set(Math.floor(new Date(t.tradedAt).getTime() / 1000), t.price)
    }
    return [...bySecond.entries()]
      .sort((a, b) => a[0] - b[0])
      .map(([time, value]) => ({ time: time as UTCTimestamp, value }))
  }, [trades])

  const rising = (points.at(-1)?.value ?? baseline) >= baseline
  const color = rising ? UP : DOWN

  useEffect(() => {
    if (!boxRef.current) return

    const chart = createChart(boxRef.current, {
      autoSize: true,
      layout: {
        background: { color: 'transparent' },
        textColor: '#9CA3AF',
        fontFamily: 'Pretendard, sans-serif',
        fontSize: 10,
      },
      grid: {
        vertLines: { visible: false },
        horzLines: { color: '#F3F4F6' },
      },
      rightPriceScale: { borderVisible: false, scaleMargins: { top: 0.15, bottom: 0.1 } },
      timeScale: { borderVisible: false, timeVisible: true, secondsVisible: false },
      crosshair: { horzLine: { labelBackgroundColor: '#191F28' }, vertLine: { labelBackgroundColor: '#191F28' } },
      handleScale: false,
      handleScroll: false,
    })

    seriesRef.current = chart.addSeries(AreaSeries, {
      lineWidth: 2,
      priceLineVisible: false,
      lastValueVisible: false,
      priceFormat: { type: 'price', precision: 0, minMove: 1 },
    })
    chartRef.current = chart

    return () => {
      chart.remove()
      chartRef.current = null
      seriesRef.current = null
    }
  }, [])

  useEffect(() => {
    const series = seriesRef.current
    if (!series) return

    series.applyOptions({
      lineColor: color,
      topColor: rising ? 'rgba(232,57,62,0.22)' : 'rgba(0,117,232,0.22)',
      bottomColor: 'rgba(255,255,255,0)',
    })
    series.setData(points)
  }, [points, color, rising])

  if (points.length === 0) {
    return (
      <div className="flex h-full items-center justify-center text-[12px] text-foreground-disabled">
        체결이 쌓이면 가격 추이가 그려집니다
      </div>
    )
  }

  return <div ref={boxRef} className="h-full w-full" />
}
