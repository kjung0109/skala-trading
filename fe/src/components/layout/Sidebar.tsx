import { BarChart3, CandlestickChart, Receipt, Wallet } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { cn } from '../../lib/cn'

const NAV = [
  { to: '/invest', label: '주문', icon: CandlestickChart },
  { to: '/market', label: '시세', icon: BarChart3 },
  { to: '/assets', label: '자산', icon: Wallet },
  { to: '/history', label: '내역', icon: Receipt },
]

export function Sidebar() {
  return (
    <nav className="flex w-sidebar shrink-0 flex-col items-center gap-1 border-r border-stroke bg-surface py-3">
      {NAV.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) =>
            cn(
              'flex w-[52px] flex-col items-center gap-1 rounded-xl py-2 transition',
              isActive
                ? 'bg-primary-light text-primary'
                : 'text-foreground-tertiary hover:bg-surface-muted hover:text-foreground-secondary',
            )
          }
        >
          <Icon size={19} strokeWidth={2.1} />
          <span className="text-[10px] font-bold">{label}</span>
        </NavLink>
      ))}
    </nav>
  )
}
