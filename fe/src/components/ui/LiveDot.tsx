import { cn } from '../../lib/cn'

type Props = { active?: boolean; className?: string }

/** 실시간 연결 표시. 살아 있으면 점이 숨쉬듯 깜빡인다. */
export function LiveDot({ active = true, className }: Props) {
  return (
    <span
      className={cn(
        'inline-flex h-1.5 w-1.5 shrink-0 rounded-full',
        active ? 'animate-pulse-dot bg-live' : 'bg-foreground-disabled',
        className,
      )}
    />
  )
}
