import type { ApiResponse } from './types'

const TOKEN_KEY = 'skala-trading-token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

/** 백엔드가 내려준 오류 메시지를 그대로 들고 다니는 예외 */
export class ApiError extends Error {
  code: string
  status: number

  constructor(code: string, message: string, status: number) {
    super(message)
    this.code = code
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = tokenStore.get()

  const res = await fetch(`/api${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      // 쿠키 대신 헤더로 보낸다. 쿠키는 오리진이 다르면 제약이 많다.
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  })

  const text = await res.text()
  const json: ApiResponse<T> = text ? JSON.parse(text) : { result: 'success' }

  if (!res.ok || json.result === 'fail') {
    // 화면에 문구를 따로 두지 않고 서버 메시지를 그대로 보여준다.
    // 검증 실패는 어떤 항목이 틀렸는지가 body에 따로 담겨 오므로 함께 붙인다.
    const message = json.error?.message ?? '요청에 실패했습니다'
    const details = Array.isArray(json.body) ? (json.body as string[]).join(', ') : null
    throw new ApiError(
      json.error?.code ?? 'UNKNOWN',
      details ? `${message} (${details})` : message,
      res.status,
    )
  }
  return json.body as T
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
}
