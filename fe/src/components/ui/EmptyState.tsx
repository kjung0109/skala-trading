import type { LucideIcon } from 'lucide-react'
import { cn } from '../../lib/cn'

type Props = {
  icon?: LucideIcon
  message: string
  sub?: string
  className?: string
}

/** 데이터가 없을 때. 빈 표를 그냥 두면 고장인지 비어 있는 건지 알 수 없다. */
export function EmptyState({ icon: Icon, message, sub, className }: Props) {
  return (
    <div className={cn('flex flex-col items-center justify-center gap-2 p-10 text-center', className)}>
      {Icon && <Icon className="h-8 w-8 text-foreground-disabled" strokeWidth={1.5} />}
      <p className="text-[13px] font-semibold text-foreground-disabled">{message}</p>
      {sub && <p className="text-[11px] text-foreground-disabled">{sub}</p>}
    </div>
  )
}
