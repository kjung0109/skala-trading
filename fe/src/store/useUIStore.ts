import { create } from 'zustand'

export type ToastTone = 'success' | 'error' | 'info'
export type Toast = { id: number; tone: ToastTone; message: string }

type UIState = {
  /** 지금 보고 있는 종목. 시세·호가·주문 패널이 함께 참조한다. */
  selectedStockId: number | null
  selectStock: (stockId: number) => void

  /**
   * 호가창에서 클릭한 가격. 주문 패널이 이걸 받아 입력란을 채운다.
   * 같은 가격을 다시 눌러도 반응하도록 seq를 함께 올린다.
   */
  pickedPrice: { price: number; seq: number } | null
  pickPrice: (price: number) => void

  toasts: Toast[]
  toast: (tone: ToastTone, message: string) => void
  dismissToast: (id: number) => void
}

let seq = 0

export const useUIStore = create<UIState>((set) => ({
  selectedStockId: null,
  selectStock: (stockId) => set({ selectedStockId: stockId }),

  pickedPrice: null,
  pickPrice: (price) =>
    set((s) => ({ pickedPrice: { price, seq: (s.pickedPrice?.seq ?? 0) + 1 } })),

  toasts: [],
  toast: (tone, message) => {
    const id = ++seq
    set((s) => ({ toasts: [...s.toasts, { id, tone, message }] }))
    // 화면 코드가 타이머를 관리하지 않도록 스토어가 스스로 걷어간다.
    setTimeout(() => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })), 3200)
  },
  dismissToast: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}))
