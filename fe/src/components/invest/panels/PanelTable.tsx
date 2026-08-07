import type { ReactNode } from 'react'
import { cn } from '../../../lib/cn'

/** 하단 탭 패널 공통 본문. 표가 길어지면 패널 안에서만 스크롤한다. */
export function PanelBody({ children }: { children: ReactNode }) {
  return <div className="min-h-0 flex-1 overflow-auto">{children}</div>
}

export function PanelEmpty({ message, sub }: { message: string; sub?: string }) {
  return (
    <div className="flex min-h-0 flex-1 flex-col items-center justify-center gap-1 p-6 text-center">
      <p className="text-[12px] font-semibold text-foreground-disabled">{message}</p>
      {sub && <p className="text-[10px] text-foreground-disabled">{sub}</p>}
    </div>
  )
}

export function Th({
  children,
  align = 'center',
  className,
}: {
  children?: ReactNode
  align?: 'left' | 'center' | 'right'
  className?: string
}) {
  return (
    <th
      className={cn(
        'py-1.5 text-[10px] font-bold',
        align === 'left' && 'pl-3 text-left',
        align === 'right' && 'pr-3 text-right',
        align === 'center' && 'text-center',
        className,
      )}
    >
      {children}
    </th>
  )
}
