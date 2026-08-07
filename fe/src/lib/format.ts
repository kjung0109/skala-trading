export const num = (n: number) => n.toLocaleString('ko-KR')
export const won = (n: number) => `${num(n)}원`
export const signed = (n: number) => `${n > 0 ? '+' : ''}${num(n)}`
export const rate = (n: number) => `${n > 0 ? '+' : ''}${n.toFixed(2)}%`

/**
 * 상승·하락·보합 텍스트 색 (국내 증시 관례: 상승 빨강 / 하락 파랑).
 * 뱃지나 부호까지 필요하면 PriceChange 컴포넌트를 쓴다.
 */
export const toneOf = (n: number) =>
  n > 0 ? 'text-up' : n < 0 ? 'text-down' : 'text-foreground-tertiary'

export const timeOf = (iso: string) => iso.slice(11, 19)
export const dateOf = (iso: string) => iso.slice(0, 10)
