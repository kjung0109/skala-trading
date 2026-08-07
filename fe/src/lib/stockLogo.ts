/**
 * 종목 아이콘 테마.
 *
 * 로고 이미지를 쓰지 않고 종목코드를 해시해 팔레트에서 고른다.
 * 같은 종목은 화면이 바뀌어도 항상 같은 색으로 보인다.
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

/** 종목명 첫 글자. 영문이면 대문자로. */
export function stockInitial(name: string) {
  return name.trim().charAt(0).toUpperCase()
}
