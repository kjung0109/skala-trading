import { cn } from '../../lib/cn'
import { stockInitial, stockLogoClass } from '../../lib/stockLogo'

type Props = {
  code: string
  name: string
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

const SIZES = {
  sm: 'h-7 w-7 text-[9px]',
  md: 'h-9 w-9 text-[11px]',
  lg: 'h-11 w-11 text-[13px]',
}

/** 종목 아이콘. 종목명 이니셜 + 코드 해시 색. */
export function StockAvatar({ code, name, size = 'md', className }: Props) {
  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center justify-center rounded-full border font-extrabold tracking-[-0.06em]',
        SIZES[size],
        stockLogoClass(code),
        className,
      )}
      aria-hidden
    >
      {stockInitial(name)}
    </span>
  )
}
