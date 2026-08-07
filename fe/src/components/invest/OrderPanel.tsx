import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Minus, Plus } from 'lucide-react'
import { useEffect, useState } from 'react'
import { ApiError } from '../../api/client'
import { accountApi, orderApi } from '../../api/endpoints'
import type { OrderSide, OrderType, Stock } from '../../api/types'
import { cn } from '../../lib/cn'
import { num, won } from '../../lib/format'
import { qk } from '../../lib/queryClient'
import { useUIStore } from '../../store/useUIStore'
import { Button } from '../ui/Button'
import { Panel } from '../ui/Panel'

type Props = { stock: Stock }

export function OrderPanel({ stock }: Props) {
  const qc = useQueryClient()
  const toast = useUIStore((s) => s.toast)

  const [side, setSide] = useState<OrderSide>('BUY')
  const [type, setType] = useState<OrderType>('LIMIT')
  const [price, setPrice] = useState(stock.currentPrice)
  const [quantity, setQuantity] = useState(10)

  const { data: account } = useQuery({ queryKey: qk.me, queryFn: accountApi.me })

  // 종목을 바꾸면 그 종목의 현재가로 다시 채운다.
  // 시세가 갱신될 때마다 덮어쓰면 입력 중인 값이 사라지므로 id에만 반응시킨다.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => setPrice(stock.currentPrice), [stock.id])

  // 호가창에서 가격을 클릭하면 입력란을 채운다.
  // 상태를 구독해 파생시키면 "값이 같으면 반응하지 않는" 문제가 생기므로
  // 클릭이라는 사건 자체를 스토어 구독으로 받는다.
  useEffect(
    () =>
      useUIStore.subscribe((s, prev) => {
        if (s.pickedPrice && s.pickedPrice.seq !== prev.pickedPrice?.seq) {
          setPrice(s.pickedPrice.price)
        }
      }),
    [],
  )

  const isMarket = type === 'MARKET'
  const effectivePrice = isMarket ? stock.currentPrice : price
  const held = account?.holdings.find((h) => h.stockId === stock.id)?.quantity ?? 0
  const maxBuyable = effectivePrice > 0 ? Math.floor((account?.balance ?? 0) / effectivePrice) : 0
  const maxQuantity = side === 'BUY' ? maxBuyable : held
  const estimated = effectivePrice * quantity

  const tick = Math.max(1, Math.round(stock.currentPrice * 0.001))

  const placeOrder = useMutation({
    mutationFn: () =>
      orderApi.place({
        stockId: stock.id,
        side,
        type,
        // 시장가는 가격을 보내지 않는다. 서버가 호가를 훑어 체결가를 정한다.
        price: isMarket ? undefined : price,
        quantity,
      }),
    onSuccess: (result) => {
      toast(result.tradedQuantity > 0 ? 'success' : 'info', result.message)
      qc.invalidateQueries({ queryKey: qk.me })
      qc.invalidateQueries({ queryKey: qk.myOrders })
      qc.invalidateQueries({ queryKey: qk.myTrades })
      qc.invalidateQueries({ queryKey: qk.orderBook(stock.id) })
      qc.invalidateQueries({ queryKey: qk.trades(stock.id) })
      qc.invalidateQueries({ queryKey: qk.stocks })
      qc.invalidateQueries({ queryKey: qk.myAudit })
    },
    onError: (e) => {
      toast('error', e instanceof ApiError ? e.message : '주문에 실패했습니다')
      // 거절된 주문도 AOP가 기록을 남긴다. 실패했을 때도 새로 읽어와야 보인다.
      qc.invalidateQueries({ queryKey: qk.myAudit })
    },
  })

  const invalid = quantity < 1 || (!isMarket && price < 1)

  return (
    <Panel title="주문" className="w-[300px] shrink-0" bodyClassName="flex flex-col p-3 gap-3">
      <Segmented
        options={[
          { value: 'BUY', label: '매수' },
          { value: 'SELL', label: '매도' },
        ]}
        value={side}
        onChange={setSide}
        activeClassName={side === 'BUY' ? 'bg-up text-white' : 'bg-down text-white'}
      />

      <Segmented
        options={[
          { value: 'LIMIT', label: '지정가' },
          { value: 'MARKET', label: '시장가' },
        ]}
        value={type}
        onChange={setType}
        activeClassName="bg-surface text-primary shadow-control"
        size="sm"
      />

      <Field label="주문 가격">
        <Stepper
          value={isMarket ? 0 : price}
          onChange={setPrice}
          step={tick}
          disabled={isMarket}
          placeholder={isMarket ? '시장가' : undefined}
          suffix="원"
        />
      </Field>

      <Field
        label="주문 수량"
        hint={
          side === 'BUY'
            ? `최대 ${num(maxBuyable)}주`
            : `보유 ${num(held)}주`
        }
      >
        <Stepper value={quantity} onChange={setQuantity} step={1} suffix="주" />
        <div className="mt-1.5 grid grid-cols-4 gap-1">
          {[10, 25, 50, 100].map((pct) => (
            <button
              key={pct}
              onClick={() => setQuantity(Math.max(1, Math.floor((maxQuantity * pct) / 100)))}
              disabled={maxQuantity < 1}
              className="rounded-md border border-stroke-input py-1 text-[11px] font-bold text-foreground-secondary transition hover:border-primary hover:text-primary disabled:opacity-40"
            >
              {pct === 100 ? '최대' : `${pct}%`}
            </button>
          ))}
        </div>
      </Field>

      <div className="mt-auto space-y-2 rounded-lg bg-surface-subtle p-3 text-[12px] tnum">
        <Row label={isMarket ? '예상 금액' : '주문 금액'} value={won(estimated)} strong />
        <Row label="주문 후 예수금" value={won(Math.max(0, (account?.balance ?? 0) - (side === 'BUY' ? estimated : -estimated)))} />
        {isMarket && (
          <p className="text-[11px] leading-relaxed font-medium text-foreground-tertiary">
            시장가는 호가를 훑어 체결되므로 실제 체결가가 다를 수 있고, 남은 수량은 소멸합니다.
          </p>
        )}
      </div>

      <Button
        size="lg"
        variant={side === 'BUY' ? 'up' : 'down'}
        disabled={invalid || placeOrder.isPending}
        onClick={() => placeOrder.mutate()}
      >
        {placeOrder.isPending ? '처리 중…' : `${num(quantity)}주 ${side === 'BUY' ? '매수' : '매도'}`}
      </Button>
    </Panel>
  )
}

function Field({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: React.ReactNode
}) {
  return (
    <div>
      <div className="mb-1.5 flex items-baseline justify-between">
        <span className="text-[11px] font-bold text-foreground-tertiary">{label}</span>
        {hint && <span className="text-[11px] font-bold text-primary tnum">{hint}</span>}
      </div>
      {children}
    </div>
  )
}

function Stepper({
  value,
  onChange,
  step,
  disabled,
  suffix,
  placeholder,
}: {
  value: number
  onChange: (v: number) => void
  step: number
  disabled?: boolean
  suffix: string
  placeholder?: string
}) {
  return (
    <div
      className={cn(
        'flex items-center rounded-lg border border-stroke-input bg-surface',
        'focus-within:border-primary',
        disabled && 'bg-surface-muted',
      )}
    >
      <StepButton onClick={() => onChange(Math.max(0, value - step))} disabled={disabled}>
        <Minus size={14} />
      </StepButton>
      <div className="flex min-w-0 flex-1 items-baseline justify-end gap-1">
        <input
          type="text"
          inputMode="numeric"
          disabled={disabled}
          value={disabled ? '' : value.toLocaleString('ko-KR')}
          placeholder={placeholder}
          onChange={(e) => onChange(Number(e.target.value.replace(/[^0-9]/g, '')) || 0)}
          className="w-full min-w-0 bg-transparent text-right text-[15px] font-bold outline-none tnum placeholder:text-foreground-disabled"
        />
        {!disabled && (
          <span className="text-[11px] font-bold text-foreground-tertiary">{suffix}</span>
        )}
      </div>
      <StepButton onClick={() => onChange(value + step)} disabled={disabled}>
        <Plus size={14} />
      </StepButton>
    </div>
  )
}

function StepButton({
  children,
  onClick,
  disabled,
}: {
  children: React.ReactNode
  onClick: () => void
  disabled?: boolean
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className="flex h-9 w-9 shrink-0 items-center justify-center text-foreground-tertiary transition hover:text-foreground disabled:opacity-30"
    >
      {children}
    </button>
  )
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className="flex items-center justify-between">
      <span className="font-semibold text-foreground-tertiary">{label}</span>
      <span className={cn('font-bold', strong ? 'text-foreground' : 'text-foreground-secondary')}>
        {value}
      </span>
    </div>
  )
}

function Segmented<T extends string>({
  options,
  value,
  onChange,
  activeClassName,
  size = 'md',
}: {
  options: { value: T; label: string }[]
  value: T
  onChange: (v: T) => void
  activeClassName: string
  size?: 'sm' | 'md'
}) {
  return (
    <div className="grid grid-cols-2 gap-1 rounded-lg bg-surface-muted p-1">
      {options.map((o) => (
        <button
          key={o.value}
          onClick={() => onChange(o.value)}
          className={cn(
            'rounded-md font-bold transition',
            size === 'sm' ? 'py-1.5 text-[12px]' : 'py-2 text-[14px]',
            value === o.value ? activeClassName : 'text-foreground-tertiary hover:text-foreground',
          )}
        >
          {o.label}
        </button>
      ))}
    </div>
  )
}
