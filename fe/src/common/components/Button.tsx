import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'outline' | 'up' | 'down'

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant
  fullWidth?: boolean
}

const variants: Record<Variant, string> = {
  primary: 'bg-primary text-white',
  outline: 'bg-white border border-line text-ink font-semibold',
  up: 'bg-up text-white',
  down: 'bg-down text-white',
}

export default function Button({
  variant = 'primary',
  fullWidth = true,
  className = '',
  children,
  ...props
}: Props) {
  return (
    <button
      type="button"
      className={`h-[52px] rounded-btn text-btn font-bold transition-opacity disabled:opacity-40
        ${fullWidth ? 'w-full' : ''} ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
