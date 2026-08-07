import { useState } from 'react'
import { cn } from '../../lib/cn'
import { stockInitial, stockLogoClass, stockLogoUrl } from '../../lib/stockLogo'

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

/**
 * 종목 아이콘.
 *
 * 로고 이미지를 먼저 쓰되, 파일이 없는 종목이 생겨도 화면이 깨지지 않도록
 * onError에서 이니셜 아이콘으로 떨어뜨린다. 종목이 추가돼도 손댈 곳이 없다.
 */
export function StockAvatar({ code, name, size = 'md', className }: Props) {
  const [failed, setFailed] = useState(false)

  const base = cn(
    'inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full border',
    SIZES[size],
    className,
  )

  if (failed) {
    return (
      <span
        className={cn(base, 'font-extrabold tracking-[-0.06em]', stockLogoClass(code))}
        aria-hidden
      >
        {stockInitial(name)}
      </span>
    )
  }

  return (
    <span className={cn(base, 'border-stroke bg-surface')}>
      <img
        src={stockLogoUrl(code)}
        alt=""
        aria-hidden
        loading="lazy"
        onError={() => setFailed(true)}
        className="h-full w-full scale-110 object-cover"
      />
    </span>
  )
}
