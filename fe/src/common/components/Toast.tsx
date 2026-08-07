import { useEffect } from 'react'

export type ToastState = { tone: 'ok' | 'error'; title: string; description?: string } | null

export default function Toast({ state, onClose }: { state: ToastState; onClose: () => void }) {
  useEffect(() => {
    if (!state) return
    const timer = setTimeout(onClose, 3200)
    return () => clearTimeout(timer)
  }, [state, onClose])

  if (!state) return null

  return (
    <div className="fixed inset-x-0 bottom-24 z-50 flex justify-center px-5">
      <div
        className={`w-full max-w-[390px] rounded-card px-4 py-3 shadow-[var(--shadow-float)] border-l-4 bg-white
          ${state.tone === 'ok' ? 'border-l-success' : 'border-l-up'}`}
      >
        <p className="text-body font-bold">{state.title}</p>
        {state.description && (
          <p className="text-sub text-ink-sub whitespace-pre-line mt-0.5">{state.description}</p>
        )}
      </div>
    </div>
  )
}
