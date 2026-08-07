import { cn } from '../../lib/cn'

type Props<T extends string> = {
  items: { key: T; label: string }[]
  activeKey: T
  onChange: (key: T) => void
}

/** 패널 상단 탭. 활성 탭은 아래 굵은 밑줄로 표시한다. */
export function SectionTabs<T extends string>({ items, activeKey, onChange }: Props<T>) {
  return (
    <div className="flex h-9 shrink-0 items-stretch border-b border-stroke bg-surface-subtle px-2">
      {items.map((item) => (
        <button
          key={item.key}
          onClick={() => onChange(item.key)}
          className={cn(
            'flex h-full items-center px-3 text-[12px] transition-colors',
            activeKey === item.key
              ? 'border-b-2 border-primary font-bold text-primary'
              : 'font-semibold text-foreground-disabled hover:text-foreground-secondary',
          )}
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}
