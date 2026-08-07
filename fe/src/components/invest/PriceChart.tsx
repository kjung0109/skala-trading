import {
  AreaSeries,
  createChart,
  type IChartApi,
  type ISeriesApi,
  type UTCTimestamp,
} from 'lightweight-charts'
import { useEffect, useMemo, useRef } from 'react'
import type { Trade } from '../../api/types'
import { useUIStore } from '../../store/useUIStore'

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
  const theme = useUIStore((s) => s.theme)
  const boxRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Area'> | null>(null)

  const points = useMemo(() => {
    const bySecond = new Map<number, number>()
    // 서버는 최신순으로 내려주므로 뒤집어 시간 오름차순으로 만든다.
    //
    // tradedAt은 시간대가 없는 한국 시각 문자열이다. 그냥 파싱하면 KST로 해석되는데
    // 차트는 UTC 기준으로 축을 그려서 체결 테이프보다 9시간 이르게 표시된다.
    // 문자열을 UTC로 읽히게 해 축 눈금이 화면의 다른 시각과 맞게 한다.
    for (const t of [...trades].reverse()) {
      bySecond.set(Math.floor(Date.parse(`${t.tradedAt}Z`) / 1000), t.price)
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
        fontFamily: 'Pretendard, sans-serif',
        fontSize: 10,
        // 차트 위에 뜨는 TradingView 로고를 끈다.
        // 라이선스는 로고 대신 tradingview.com 링크를 두는 것도 인정하므로,
        // 차트 아래에 출처 표기를 따로 뒀다(ChartAttribution).
        attributionLogo: false,
      },
      rightPriceScale: { borderVisible: false, scaleMargins: { top: 0.15, bottom: 0.1 } },
      timeScale: { borderVisible: false, timeVisible: true, secondsVisible: false },
      // 차트는 캔버스라 CSS 변수를 따라오지 않는다. 색은 테마에 맞춰 직접 넣는다.
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
    const chart = chartRef.current
    const series = seriesRef.current
    if (!chart || !series) return

    const dark = theme === 'dark'
    chart.applyOptions({
      layout: { textColor: dark ? '#8A8D93' : '#9CA3AF' },
      grid: {
        vertLines: { visible: false },
        horzLines: { color: dark ? '#252836' : '#F3F4F6' },
      },
      crosshair: {
        horzLine: { labelBackgroundColor: dark ? '#3A3D4A' : '#191F28' },
        vertLine: { labelBackgroundColor: dark ? '#3A3D4A' : '#191F28' },
      },
    })

    series.applyOptions({
      lineColor: color,
      topColor: rising ? 'rgba(232,57,62,0.22)' : 'rgba(0,117,232,0.22)',
      bottomColor: dark ? 'rgba(24,27,36,0)' : 'rgba(255,255,255,0)',
    })
    series.setData(points)
  }, [points, color, rising, theme])

  // 컨테이너를 조건부로 렌더하면 안 된다.
  // 체결이 없는 동안 ref가 비어 생성 이펙트가 그냥 종료되고, deps가 비어 있어 다시 시도하지 않는다.
  // 그래서 데이터가 들어와도 빈 상자만 남았다. 상자는 항상 두고 안내 문구를 위에 겹친다.
  return (
    <div className="relative h-full w-full">
      <div ref={boxRef} className="h-full w-full" />
      {points.length === 0 && (
        <div className="absolute inset-0 flex items-center justify-center text-[12px] text-foreground-disabled">
          체결이 쌓이면 가격 추이가 그려집니다
        </div>
      )}
    </div>
  )
}

/**
 * lightweight-charts 라이선스가 요구하는 출처 표기.
 * 차트 위 로고를 껐으므로 이 링크가 그 자리를 대신한다.
 */
export function ChartAttribution() {
  return (
    <a
      href="https://www.tradingview.com/"
      target="_blank"
      rel="noreferrer"
      className="text-[9px] font-medium text-foreground-disabled transition hover:text-foreground-tertiary"
    >
      Charts by TradingView
    </a>
  )
}
