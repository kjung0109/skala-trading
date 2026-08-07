import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'

type PanelProps = {
  title?: ReactNode
  action?: ReactNode
  children: ReactNode
  className?: string
  /** 본문이 넘칠 때 패널 안에서만 스크롤되게 한다. */
  scroll?: boolean
  bodyClassName?: string
}

/** 데스크톱 화면을 이루는 기본 단위. 헤더 + 본문 한 벌. */
export function Panel({ title, action, children, className, scroll, bodyClassName }: PanelProps) {
  return (
    <section
      className={cn(
        'flex min-h-0 flex-col overflow-hidden rounded-xl border border-stroke bg-surface',
        className,
      )}
    >
      {title && (
        <header className="flex h-11 shrink-0 items-center justify-between border-b border-stroke-subtle px-4">
          <h2 className="text-[13px] font-bold text-foreground">{title}</h2>
          {action}
        </header>
      )}
      <div className={cn('min-h-0 flex-1', scroll && 'overflow-y-auto', bodyClassName)}>
        {children}
      </div>
    </section>
  )
}
