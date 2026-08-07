/**
 * 종목 아이콘 테마.
 *
 * 실제 로고 이미지는 쓰지 않는다. 상표를 임의로 가져다 쓰는 셈이 되고,
 * 종목이 늘 때마다 이미지를 챙겨야 한다. 대신 종목코드를 해시해 팔레트에서 고른다.
 * 같은 종목은 어느 화면에서든 항상 같은 색으로 보인다.
 */
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
 * 아이콘에 넣을 이니셜.
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
