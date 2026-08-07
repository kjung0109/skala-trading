import { cn } from '../../lib/cn'
import { stockInitial, stockLogoClass } from '../../lib/stockLogo'

type Props = {
  code: string
  name: string
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

const SIZES = {
  sm: 'h-7 w-7 text-[12px] rounded-lg',
  md: 'h-9 w-9 text-[14px] rounded-[10px]',
  lg: 'h-11 w-11 text-[17px] rounded-xl',
}

/** 종목 아이콘. 실제 로고 대신 종목명 첫 글자 + 코드 해시 색을 쓴다. */
export function StockAvatar({ code, name, size = 'md', className }: Props) {
  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center justify-center border font-extrabold',
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
