export const num = (n: number) => n.toLocaleString('ko-KR')
export const won = (n: number) => `${num(n)}원`
export const signed = (n: number) => `${n > 0 ? '+' : ''}${num(n)}`
export const rate = (n: number) => `${n > 0 ? '+' : ''}${n.toFixed(2)}%`

/** 억/만 단위로 줄여 쓴다. 총자산처럼 자릿수가 큰 값에 쓴다. */
export function compactWon(n: number) {
  const abs = Math.abs(n)
  if (abs >= 100_000_000) return `${(n / 100_000_000).toFixed(2)}억`
  if (abs >= 10_000) return `${Math.round(n / 10_000).toLocaleString('ko-KR')}만`
  return num(n)
}

/** 상승·하락·보합 텍스트 색 (국내 증시 관례: 상승 빨강 / 하락 파랑) */
export const toneOf = (n: number) =>
  n > 0 ? 'text-up' : n < 0 ? 'text-down' : 'text-foreground-tertiary'

/** 상승·하락 뱃지 배경/보더까지 포함한 조합 */
export const badgeToneOf = (n: number) =>
  n > 0
    ? 'text-up bg-up-bg border-up-border'
    : n < 0
      ? 'text-down bg-down-bg border-down-border'
      : 'text-foreground-tertiary bg-surface-muted border-stroke'

export const arrowOf = (n: number) => (n > 0 ? '▲' : n < 0 ? '▼' : '–')

export const timeOf = (iso: string) => iso.slice(11, 19)
export const dateOf = (iso: string) => iso.slice(0, 10)
