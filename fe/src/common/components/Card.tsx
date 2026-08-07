import type { ReactNode } from 'react'

export default function Card({
  children,
  className = '',
  title,
  right,
}: {
  children: ReactNode
  className?: string
  title?: string
  right?: ReactNode
}) {
  return (
    <section className={`bg-white rounded-card-lg shadow-[var(--shadow-card)] ${className}`}>
      {title && (
        <header className="flex items-center justify-between px-5 pt-4 pb-2">
          <h2 className="text-md font-bold">{title}</h2>
          {right}
        </header>
      )}
      {children}
    </section>
  )
}
