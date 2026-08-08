import {
  CandlestickSeries,
  createChart,
  HistogramSeries,
  type IChartApi,
  type ISeriesApi,
  type UTCTimestamp,
} from 'lightweight-charts'
import { useEffect, useMemo, useRef } from 'react'
import type { Candle } from '../../api/types'
import { useUIStore } from '../../store/useUIStore'

type Props = {
  candles: Candle[]
  /** 전일 종가. 기준선으로 그리고 캔들 색의 기준이 된다. */
  previousClose: number
}

// 국내 증시 관례 — 상승 빨강, 하락 파랑
const UP = '#E8393E'
const DOWN = '#0075E8'

/**
 * 가격 추이 캔들 차트.
 *
 * 외부 시세 API를 쓰지 않으므로 캔들도 우리 체결 데이터로 만든다.
 * 구간을 접어 시가·고가·저가·종가를 뽑는 일은 서버가 한다.
 */
export function PriceChart({ candles, previousClose }: Props) {
  const theme = useUIStore((s) => s.theme)
  const boxRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const candleRef = useRef<ISeriesApi<'Candlestick'> | null>(null)
  const volumeRef = useRef<ISeriesApi<'Histogram'> | null>(null)

  const { bars, volumes } = useMemo(() => {
    // time은 시간대가 없는 한국 시각 문자열이다. 그냥 파싱하면 KST로 해석되는데
    // 차트는 UTC 기준으로 축을 그려서 체결 테이프보다 9시간 이르게 표시된다.
    // 문자열을 UTC로 읽히게 해 축 눈금이 화면의 다른 시각과 맞게 한다.
    const toTime = (iso: string) => (Date.parse(`${iso}Z`) / 1000) as UTCTimestamp

    return {
      bars: candles.map((c) => ({
        time: toTime(c.time),
        open: c.open,
        high: c.high,
        low: c.low,
        close: c.close,
      })),
      // 거래량 막대도 캔들과 같은 색으로 칠해 두 영역을 눈으로 잇는다.
      volumes: candles.map((c) => ({
        time: toTime(c.time),
        value: c.volume,
        color: c.close >= c.open ? 'rgba(232,57,62,0.4)' : 'rgba(0,117,232,0.4)',
      })),
    }
  }, [candles])

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
      rightPriceScale: {
        borderVisible: false,
        // 아래 25%는 거래량 자리로 비워둔다.
        scaleMargins: { top: 0.08, bottom: 0.27 },
      },
      timeScale: { borderVisible: false, timeVisible: true, secondsVisible: true },
      handleScale: { axisPressedMouseMove: false },
      handleScroll: { horzTouchDrag: false, vertTouchDrag: false },
    })

    candleRef.current = chart.addSeries(CandlestickSeries, {
      upColor: UP,
      downColor: DOWN,
      borderUpColor: UP,
      borderDownColor: DOWN,
      wickUpColor: UP,
      wickDownColor: DOWN,
      priceFormat: { type: 'price', precision: 0, minMove: 1 },
    })

    volumeRef.current = chart.addSeries(HistogramSeries, {
      priceFormat: { type: 'volume' },
      // 별도 축에 붙여 가격과 눈금을 공유하지 않게 한다.
      priceScaleId: 'volume',
      lastValueVisible: false,
      priceLineVisible: false,
    })
    chart.priceScale('volume').applyOptions({
      scaleMargins: { top: 0.78, bottom: 0 },
    })

    chartRef.current = chart

    return () => {
      chart.remove()
      chartRef.current = null
      candleRef.current = null
      volumeRef.current = null
    }
  }, [])

  useEffect(() => {
    const chart = chartRef.current
    const candleSeries = candleRef.current
    const volumeSeries = volumeRef.current
    if (!chart || !candleSeries || !volumeSeries) return

    const dark = theme === 'dark'
    chart.applyOptions({
      layout: { textColor: dark ? '#8A8D93' : '#9CA3AF' },
      grid: {
        vertLines: { color: dark ? '#20232F' : '#F6F7F9' },
        horzLines: { color: dark ? '#252836' : '#F3F4F6' },
      },
      crosshair: {
        horzLine: { labelBackgroundColor: dark ? '#3A3D4A' : '#191F28' },
        vertLine: { labelBackgroundColor: dark ? '#3A3D4A' : '#191F28' },
      },
    })

    candleSeries.setData(bars)
    volumeSeries.setData(volumes)

    // 받아온 구간 전체가 화면에 들어오게 맞춘다. 이걸 하지 않으면 기본 배율 때문에
    // 최근 몇 개 봉만 크게 보이고 나머지가 화면 밖으로 밀린다.
    chart.timeScale().fitContent()
  }, [bars, volumes, theme])

  // 전일 종가 기준선. 실제 증권 화면에서 오늘의 등락을 가늠하는 기준이다.
  useEffect(() => {
    const candleSeries = candleRef.current
    if (!candleSeries || !previousClose) return

    const line = candleSeries.createPriceLine({
      price: previousClose,
      color: theme === 'dark' ? '#55585E' : '#9CA3AF',
      lineWidth: 1,
      lineStyle: 2, // dashed
      axisLabelVisible: true,
      title: '전일',
    })
    return () => candleSeries.removePriceLine(line)
  }, [previousClose, theme])

  return (
    <div className="relative h-full w-full">
      {/* 컨테이너를 조건부로 렌더하면 안 된다. 체결이 없는 동안 ref가 비어 생성 이펙트가
          그냥 종료되고, deps가 비어 있어 다시 시도하지 않는다. 상자는 항상 두고 문구를 겹친다. */}
      <div ref={boxRef} className="h-full w-full" />
      {bars.length === 0 && (
        <div className="absolute inset-0 flex items-center justify-center text-[12px] text-foreground-disabled">
          체결이 쌓이면 캔들이 그려집니다
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
