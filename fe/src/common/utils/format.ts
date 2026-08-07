export const won = (n: number) => `${n.toLocaleString('ko-KR')}원`
export const num = (n: number) => n.toLocaleString('ko-KR')
export const signed = (n: number) => `${n > 0 ? '+' : ''}${n.toLocaleString('ko-KR')}`
export const rate = (n: number) => `${n > 0 ? '+' : ''}${n.toFixed(2)}%`

/** 상승·하락·보합에 따른 색 (국내 증시 관례) */
export const toneOf = (n: number) => (n > 0 ? 'text-up' : n < 0 ? 'text-down' : 'text-ink-hint')

export const timeOf = (iso: string) => iso.slice(11, 19)
