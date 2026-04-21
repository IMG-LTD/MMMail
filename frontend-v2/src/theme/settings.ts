import { lt, type LocalizedText } from '@/locales'

export type ThemeScheme = 'light' | 'dark' | 'auto'
export type ResolvedThemeScheme = 'light' | 'dark'
export type ThemeDensity = 'comfortable' | 'compact'
export type ThemePresetId = 'swiss' | 'graphite' | 'signal'

export interface ThemePreset {
  id: ThemePresetId
  label: LocalizedText
  description: LocalizedText
  accent: string
  accentPressed: string
}

export const themePresets: ThemePreset[] = [
  {
    id: 'swiss',
    label: lt('瑞士蓝', '瑞士藍', 'Swiss Blue'),
    description: lt('MMMail 默认平衡配色，适合中性界面与邮件优先场景。', 'MMMail 預設平衡配色，適合中性介面與郵件優先場景。', 'Default MMMail balance for neutral surfaces and mail-first accents.'),
    accent: '#4f6bed',
    accentPressed: '#3b57db'
  },
  {
    id: 'graphite',
    label: lt('石墨灰', '石墨灰', 'Graphite'),
    description: lt('更克制的钢灰强调色，适合高密度运营工作区。', '更克制的鋼灰強調色，適合高密度營運工作區。', 'A quieter steel-toned accent for dense operational workspaces.'),
    accent: '#5d708d',
    accentPressed: '#435267'
  },
  {
    id: 'signal',
    label: lt('信号琥珀', '訊號琥珀', 'Signal Amber'),
    description: lt('更温暖的动作强调色，适合审计、告警和响应流程。', '更溫暖的動作強調色，適合稽核、告警和回應流程。', 'Warmer action emphasis for audits, alerts, and response workflows.'),
    accent: '#d48c00',
    accentPressed: '#b87700'
  }
]

export const themeSchemes = [
  { value: 'light' as const, label: lt('浅色', '淺色', 'Light') },
  { value: 'dark' as const, label: lt('深色', '深色', 'Dark') },
  { value: 'auto' as const, label: lt('自动', '自動', 'Auto') }
]

export const densityModes = [
  { value: 'comfortable' as const, label: lt('舒适', '舒適', 'Comfortable') },
  { value: 'compact' as const, label: lt('紧凑', '緊湊', 'Compact') }
]

export const radiusOptions = [8, 10, 12, 14]

export const defaultThemeSettings = {
  density: 'comfortable' as ThemeDensity,
  radius: 12,
  themePreset: 'swiss' as ThemePresetId,
  themeScheme: 'light' as ThemeScheme
}

export const themeStorageKeys = {
  density: 'mmmail-theme-density',
  radius: 'mmmail-theme-radius',
  themePreset: 'mmmail-theme-preset',
  themeScheme: 'mmmail-theme-scheme'
}

export function resolveThemePreset(presetId: ThemePresetId) {
  return themePresets.find(preset => preset.id === presetId) ?? themePresets[0]
}
