import { useState } from 'react'
import Button from '../common/components/Button'
import { ApiError } from '../common/api/client'

const DEMO_ACCOUNTS = ['trader01', 'trader02', 'trader03']

export default function LoginSheet({
  onLogin,
  loading,
}: {
  onLogin: (accountId: string, password: string) => Promise<void>
  loading: boolean
}) {
  const [accountId, setAccountId] = useState('trader01')
  const [password, setPassword] = useState('pw1234')
  const [error, setError] = useState<string | null>(null)

  const submit = async () => {
    setError(null)
    try {
      await onLogin(accountId, password)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '로그인에 실패했습니다')
    }
  }

  return (
    <div className="fixed inset-0 z-40 flex items-end justify-center bg-black/35">
      <div className="w-full max-w-[430px] bg-white rounded-t-card-xl px-5 pt-6 pb-8">
        <h2 className="text-heading font-bold">로그인</h2>
        <p className="text-sub text-ink-sub mt-1">계좌를 선택하고 주문을 시작하세요</p>

        <div className="flex gap-2 mt-5">
          {DEMO_ACCOUNTS.map((id) => (
            <button
              key={id}
              onClick={() => setAccountId(id)}
              className={`flex-1 h-11 rounded-btn text-body font-semibold border
                ${
                  accountId === id
                    ? 'border-primary text-primary bg-primary-tint'
                    : 'border-line text-ink-sub bg-white'
                }`}
            >
              {id}
            </button>
          ))}
        </div>

        <label className="block text-caption text-ink-hint mt-5 mb-1.5">비밀번호</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && submit()}
          className="w-full h-12 rounded-btn border border-line px-4 text-md outline-none focus:border-primary"
        />

        {error && <p className="text-sub text-up mt-2">{error}</p>}

        <Button className="mt-5" onClick={submit} disabled={loading}>
          {loading ? '로그인 중…' : '로그인'}
        </Button>

        <p className="text-caption text-ink-hint text-center mt-3">
          데모 계좌 3개 · 비밀번호 pw1234
        </p>
      </div>
    </div>
  )
}
