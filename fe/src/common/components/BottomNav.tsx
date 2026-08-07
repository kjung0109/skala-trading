export type TabKey = 'market' | 'order' | 'asset' | 'history'

const TABS: { key: TabKey; label: string; icon: string }[] = [
  { key: 'market', label: '시세', icon: '📈' },
  { key: 'order', label: '주문', icon: '📝' },
  { key: 'asset', label: '내 자산', icon: '💼' },
  { key: 'history', label: '내역', icon: '🧾' },
]

export default function BottomNav({
  current,
  onChange,
}: {
  current: TabKey
  onChange: (tab: TabKey) => void
}) {
  return (
    <nav className="sticky bottom-0 z-30 bg-white/95 backdrop-blur border-t border-line">
      <ul className="grid grid-cols-4">
        {TABS.map((tab) => {
          const active = tab.key === current
          return (
            <li key={tab.key}>
              <button
                onClick={() => onChange(tab.key)}
                className="w-full flex flex-col items-center gap-0.5 py-2.5"
              >
                <span className={`text-[17px] leading-none ${active ? '' : 'opacity-40 grayscale'}`}>
                  {tab.icon}
                </span>
                <span
                  className={`text-caption ${active ? 'text-primary font-bold' : 'text-nav'}`}
                >
                  {tab.label}
                </span>
              </button>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}
