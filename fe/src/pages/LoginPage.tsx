import { Activity, LineChart, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import { ApiError } from '../api/client'
import { Button } from '../components/ui/Button'
import { cn } from '../lib/cn'
import { useAuthStore } from '../store/useAuthStore'

const DEMO = [
  { id: 'trader01', label: '1억' },
  { id: 'trader02', label: '3억' },
  { id: 'trader03', label: '5억' },
]

export function LoginPage() {
  const login = useAuthStore((s) => s.login)
  const signup = useAuthStore((s) => s.signup)
  const pending = useAuthStore((s) => s.pending)

  const [mode, setMode] = useState<'login' | 'signup'>('login')
  const [accountId, setAccountId] = useState('trader01')
  const [password, setPassword] = useState('pw1234')
  const [error, setError] = useState<string | null>(null)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await (mode === 'login' ? login : signup)(accountId.trim(), password)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '요청에 실패했습니다')
    }
  }

  return (
    <div className="flex h-screen">
      {/* 브랜드 패널 */}
      <aside className="hidden flex-1 flex-col justify-between bg-primary-gradient p-14 text-white lg:flex">
        <div className="flex items-center gap-2.5">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/15 text-[15px] font-black">
            S
          </span>
          <span className="text-[17px] font-extrabold">SKALA Trading</span>
        </div>

        <div>
          <h1 className="text-[40px] leading-[1.25] font-extrabold tracking-tight">
            호가와 체결로 움직이는
            <br />
            모의 주식 거래
          </h1>
          <p className="mt-5 max-w-[420px] text-[15px] leading-relaxed text-white/70">
            외부에서 시세를 받아오지 않습니다. 참여자가 낸 주문이 호가창에서 만나 체결되고, 그
            가격이 곧 현재가가 됩니다.
          </p>

          <ul className="mt-9 space-y-3.5">
            <Feature icon={LineChart} text="가격·시간 우선 원칙의 주문 체결 엔진" />
            <Feature icon={Activity} text="SSE로 실시간 전송되는 호가와 체결" />
            <Feature icon={ShieldCheck} text="비관적 락으로 지켜지는 동시 주문 정합성" />
          </ul>
        </div>

        <p className="text-[12px] text-white/45">SKALA KDT Back-end · 교육용 프로젝트</p>
      </aside>

      {/* 입력 패널 */}
      <main className="flex w-full items-center justify-center bg-surface px-8 lg:w-[480px] lg:shrink-0">
        <form onSubmit={submit} className="w-full max-w-[340px]">
          <h2 className="text-[24px] font-extrabold text-foreground">
            {mode === 'login' ? '로그인' : '계좌 개설'}
          </h2>
          <p className="mt-1.5 text-[13px] font-medium text-foreground-tertiary">
            {mode === 'login'
              ? '계좌 아이디와 비밀번호를 입력하세요.'
              : '새 계좌를 만들면 초기 예수금 1억 원이 지급됩니다.'}
          </p>

          <div className="mt-7 space-y-3">
            <Input
              label="계좌 아이디"
              value={accountId}
              onChange={setAccountId}
              autoComplete="username"
            />
            <Input
              label="비밀번호"
              type="password"
              value={password}
              onChange={setPassword}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            />
          </div>

          {error && (
            <p className="mt-3 rounded-lg border border-up-border bg-up-bg px-3 py-2 text-[12px] font-bold text-up">
              {error}
            </p>
          )}

          <Button
            type="submit"
            size="lg"
            className="mt-5 w-full"
            disabled={pending || !accountId.trim() || !password}
          >
            {pending ? '처리 중…' : mode === 'login' ? '로그인' : '계좌 개설하고 시작하기'}
          </Button>

          <button
            type="button"
            onClick={() => {
              setMode(mode === 'login' ? 'signup' : 'login')
              setError(null)
            }}
            className="mt-3.5 w-full text-[12px] font-bold text-foreground-tertiary transition hover:text-primary"
          >
            {mode === 'login' ? '계좌가 없으신가요? 계좌 개설하기' : '이미 계좌가 있으신가요? 로그인하기'}
          </button>

          {mode === 'login' && (
            <div className="mt-8 rounded-xl border border-stroke bg-surface-subtle p-4">
              <p className="text-[11px] font-bold text-foreground-tertiary">
                데모 계좌 (비밀번호 pw1234)
              </p>
              <div className="mt-2 grid grid-cols-3 gap-1.5">
                {DEMO.map((d) => (
                  <button
                    key={d.id}
                    type="button"
                    onClick={() => {
                      setAccountId(d.id)
                      setPassword('pw1234')
                    }}
                    className={cn(
                      'rounded-lg border py-2 text-[11px] font-bold transition',
                      accountId === d.id
                        ? 'border-primary-border bg-primary-light text-primary'
                        : 'border-stroke-input bg-surface text-foreground-secondary hover:border-primary-border',
                    )}
                  >
                    <span className="block">{d.id}</span>
                    <span className="block text-[10px] font-semibold opacity-70">{d.label}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </form>
      </main>
    </div>
  )
}

function Feature({ icon: Icon, text }: { icon: typeof LineChart; text: string }) {
  return (
    <li className="flex items-center gap-3 text-[14px] font-semibold text-white/90">
      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-white/15">
        <Icon size={16} />
      </span>
      {text}
    </li>
  )
}

function Input({
  label,
  value,
  onChange,
  type = 'text',
  autoComplete,
}: {
  label: string
  value: string
  onChange: (v: string) => void
  type?: string
  autoComplete?: string
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-[12px] font-bold text-foreground-secondary">{label}</span>
      <input
        type={type}
        value={value}
        autoComplete={autoComplete}
        onChange={(e) => onChange(e.target.value)}
        className="h-11 w-full rounded-lg border border-stroke-input bg-surface px-3.5 text-[14px] font-semibold outline-none transition focus:border-primary focus:shadow-focus-ring"
      />
    </label>
  )
}
