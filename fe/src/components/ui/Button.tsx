import type { ButtonHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

type Variant = 'primary' | 'up' | 'down' | 'ghost' | 'outline'
type Size = 'sm' | 'md' | 'lg'

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-primary text-white hover:bg-primary-hover shadow-primary-btn',
  up: 'bg-up text-white hover:brightness-95 shadow-up-btn',
  down: 'bg-down text-white hover:brightness-95 shadow-down-btn',
  outline: 'border border-stroke-input bg-surface text-foreground-secondary hover:bg-surface-muted',
  ghost: 'text-foreground-tertiary hover:bg-surface-muted',
}

const SIZES: Record<Size, string> = {
  sm: 'h-8 px-3 text-[12px] rounded-lg',
  md: 'h-10 px-4 text-[13px] rounded-lg',
  lg: 'h-12 px-5 text-[15px] rounded-xl',
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant
  size?: Size
}

export function Button({
  variant = 'primary',
  size = 'md',
  className,
  ...props
}: ButtonProps) {
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center gap-1.5 font-bold transition',
        'disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none',
        VARIANTS[variant],
        SIZES[size],
        className,
      )}
      {...props}
    />
  )
}
