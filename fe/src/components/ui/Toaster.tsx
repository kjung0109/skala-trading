import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react'
import { cn } from '../../lib/cn'
import { useUIStore, type ToastTone } from '../../store/useUIStore'

const ICONS = { success: CheckCircle2, error: AlertCircle, info: Info }
const TONES: Record<ToastTone, string> = {
  success: 'text-live',
  error: 'text-danger',
  info: 'text-primary',
}

/**
 * 토스트는 스토어가 타이머까지 관리한다.
 * 화면이 자주 리렌더돼도(시세가 초당 갱신된다) 타이머가 초기화되지 않는다.
 */
export function Toaster() {
  const toasts = useUIStore((s) => s.toasts)
  const dismiss = useUIStore((s) => s.dismissToast)

  if (toasts.length === 0) return null

  return (
    <div className="pointer-events-none fixed bottom-6 left-1/2 z-50 flex -translate-x-1/2 flex-col items-center gap-2">
      {toasts.map((t) => {
        const Icon = ICONS[t.tone]
        return (
          <div
            key={t.id}
            className="pointer-events-auto flex animate-modal-in items-center gap-2.5 rounded-xl border border-stroke bg-surface py-3 pr-2.5 pl-4 shadow-dropdown"
          >
            <Icon size={17} className={cn('shrink-0', TONES[t.tone])} />
            <span className="max-w-[420px] text-[13px] font-semibold text-foreground">
              {t.message}
            </span>
            <button
              onClick={() => dismiss(t.id)}
              className="rounded-md p-1 text-foreground-disabled hover:bg-surface-muted"
              aria-label="닫기"
            >
              <X size={14} />
            </button>
          </div>
        )
      })}
    </div>
  )
}
