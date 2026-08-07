import { cn } from '../../lib/cn'

type Props = {
  /** 등락률(%) 또는 등락액. mode로 구분한다. */
  value: number | null | undefined
  variant?: 'text' | 'badge'
  /** rate면 % 표기, amount면 금액 표기 */
  mode?: 'rate' | 'amount'
  arrow?: boolean
  className?: string
}

/**
 * 등락 표시. 국내 증시 관례로 상승은 빨강, 하락은 파랑.
 *
 * 부호·색·화살표를 매번 따로 계산하면 화면마다 미묘하게 달라진다.
 * 값 하나만 넘기면 되도록 한곳에 모았다.
 */
export function PriceChange({
  value,
  variant = 'text',
  mode = 'rate',
  arrow = false,
  className,
}: Props) {
  if (value == null || Number.isNaN(value)) {
    return <span className={cn('font-bold text-foreground-disabled', className)}>-</span>
  }

  const isUp = value > 0
  const isZero = value === 0
  const sign = isUp ? '+' : isZero ? '' : '-'
  const abs = Math.abs(value)
  const body = mode === 'rate' ? `${abs.toFixed(2)}%` : abs.toLocaleString('ko-KR')
  const mark = arrow ? (isUp ? '▲ ' : isZero ? '– ' : '▼ ') : ''
  const label = `${mark}${sign}${body}`

  const tone = isZero ? 'text-foreground-tertiary' : isUp ? 'text-up' : 'text-down'

  if (variant === 'badge') {
    const surface = isZero
      ? 'bg-surface-muted border-stroke'
      : isUp
        ? 'bg-up-bg border-up-border'
        : 'bg-down-bg border-down-border'

    return (
      <span
        className={cn(
          'inline-flex items-center rounded-md border px-1.5 py-0.5 text-[11px] font-bold tnum',
          tone,
          surface,
          className,
        )}
      >
        {label}
      </span>
    )
  }

  return <span className={cn('font-bold tnum', tone, className)}>{label}</span>
}
