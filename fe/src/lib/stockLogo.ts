/**
 * 종목 아이콘.
 *
 * public/stock-logo/{종목코드}.png 를 먼저 쓰고, 없으면 이니셜로 떨어진다.
 * 파일을 앱 안에 두는 이유는 외부 주소를 참조하면 네트워크가 없거나
 * 그쪽이 바뀌었을 때 화면이 깨지기 때문이다.
 */
export function stockLogoUrl(code: string) {
  return `/stock-logo/${code}.png`
}

/** 이미지가 없을 때 쓰는 색 테마. 종목코드를 해시해 팔레트에서 고른다. */
const THEMES = ['blue', 'green', 'purple', 'yellow', 'teal', 'orange'] as const

export type AvatarTheme = (typeof THEMES)[number]

const CLASSES: Record<AvatarTheme, string> = {
  blue: 'bg-avatar-blue-bg border-avatar-blue-border text-avatar-blue-text',
  green: 'bg-avatar-green-bg border-avatar-green-border text-avatar-green-text',
  purple: 'bg-avatar-purple-bg border-avatar-purple-border text-avatar-purple-text',
  yellow: 'bg-avatar-yellow-bg border-avatar-yellow-border text-avatar-yellow-text',
  teal: 'bg-avatar-teal-bg border-avatar-teal-border text-avatar-teal-text',
  orange: 'bg-avatar-orange-bg border-avatar-orange-border text-avatar-orange-text',
}

export function stockLogoClass(code: string) {
  const sum = [...code].reduce((acc, ch) => acc + ch.charCodeAt(0), 0)
  return CLASSES[THEMES[sum % THEMES.length]]
}

/**
 * 이미지가 없을 때 넣을 이니셜.
 * 영문·숫자는 두 글자를 대문자로, 한글은 앞 두 글자를 쓴다.
 * ("삼성전자" → 삼성 / "SK하이닉스" → SK)
 */
export function stockInitial(name: string) {
  const trimmed = name.trim()
  if (!trimmed) return '?'

  const latin = trimmed.replace(/[^A-Za-z0-9]/g, '')
  if (latin.length >= 2) return latin.slice(0, 2).toUpperCase()

  return [...trimmed.replace(/\s+/g, '')].slice(0, 2).join('')
}
