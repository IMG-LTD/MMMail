import { resolveThemePreset, type ResolvedThemeScheme, type ThemeDensity, type ThemePresetId } from '@/theme/settings'

export interface MmDesignTokens {
  appBg: string
  surface: string
  surfaceSoft: string
  surfaceMuted: string
  overlay: string
  border: string
  borderStrong: string
  focusRing: string
  textPrimary: string
  textSecondary: string
  textMuted: string
  textDisabled: string
  textInverse: string
  brandPrimary: string
  brandPrimaryHover: string
  brandSoft: string
  brandBorder: string
  brandContrast: string
  success: string
  info: string
  warning: string
  danger: string
  premium: string
  hosted: string
  preview: string
  productMail: string
  productCalendar: string
  productDrive: string
  productDocs: string
  productSheets: string
  productPass: string
  productCollaboration: string
  productCommand: string
  productNotifications: string
  productAdmin: string
  productSettings: string
  productLabs: string
  radiusXs: string
  radiusSm: string
  radiusMd: string
  radiusLg: string
  radiusXl: string
  shadowSm: string
  shadowMd: string
  shadowLg: string
  durationFast: string
  durationBase: string
  durationSlow: string
  densityFactor: string
}

interface BuildTokenInput {
  density: ThemeDensity
  preset: ThemePresetId
  radius: number
  scheme: ResolvedThemeScheme
}

function hexToRgb(hex: string) {
  const normalized = hex.replace('#', '')
  const chunk = normalized.length === 3 ? normalized.split('').map(value => `${value}${value}`).join('') : normalized
  const safeChunk = chunk.padEnd(6, '0')
  const red = Number.parseInt(safeChunk.slice(0, 2), 16)
  const green = Number.parseInt(safeChunk.slice(2, 4), 16)
  const blue = Number.parseInt(safeChunk.slice(4, 6), 16)
  return `${red}, ${green}, ${blue}`
}

function withAlpha(hex: string, alpha: number) {
  return `rgba(${hexToRgb(hex)}, ${alpha})`
}

export function buildMmDesignTokens(input: BuildTokenInput): MmDesignTokens {
  const preset = resolveThemePreset(input.preset)
  const isDark = input.scheme === 'dark'
  const radius = input.radius

  return {
    appBg: isDark ? '#0e1014' : '#f4f6f8',
    surface: isDark ? '#151820' : '#ffffff',
    surfaceSoft: isDark ? '#10141b' : '#f8fafc',
    surfaceMuted: isDark ? '#1b202b' : '#eef2f6',
    overlay: isDark ? 'rgba(3, 5, 9, 0.72)' : 'rgba(17, 24, 39, 0.42)',
    border: isDark ? '#242833' : '#e3e7ee',
    borderStrong: isDark ? '#303646' : '#d4dbe6',
    focusRing: withAlpha(preset.accent, isDark ? 0.5 : 0.34),
    textPrimary: isDark ? '#eef1f5' : '#111827',
    textSecondary: isDark ? '#c6ccd5' : '#3f4652',
    textMuted: isDark ? '#8f98a8' : '#667085',
    textDisabled: isDark ? '#697386' : '#9aa4b2',
    textInverse: '#ffffff',
    brandPrimary: preset.accent,
    brandPrimaryHover: preset.accentPressed,
    brandSoft: withAlpha(preset.accent, isDark ? 0.2 : 0.11),
    brandBorder: withAlpha(preset.accent, isDark ? 0.44 : 0.22),
    brandContrast: '#ffffff',
    success: isDark ? '#5cc99a' : '#168a66',
    info: isDark ? '#75b8f0' : '#2563eb',
    warning: isDark ? '#e8b547' : '#d97706',
    danger: isDark ? '#f1847c' : '#d92d20',
    premium: isDark ? '#c89cff' : '#8b5cf6',
    hosted: isDark ? '#7dd3fc' : '#0284c7',
    preview: isDark ? '#b8bcc2' : '#697586',
    productMail: isDark ? '#8a9cf4' : '#4f6bed',
    productCalendar: isDark ? '#5dc9a1' : '#2fa37b',
    productDrive: isDark ? '#a68dfb' : '#7a5af8',
    productDocs: isDark ? '#67b2e3' : '#2e8bcc',
    productSheets: isDark ? '#6bc477' : '#3fa04e',
    productPass: isDark ? '#f2a574' : '#e47b39',
    productCollaboration: isDark ? '#f0b86e' : '#c56a13',
    productCommand: isDark ? '#9aa1ae' : '#475467',
    productNotifications: isDark ? '#f09ab8' : '#db2777',
    productAdmin: isDark ? '#9aa1ae' : '#535b6a',
    productSettings: isDark ? '#8f98a8' : '#667085',
    productLabs: isDark ? '#b8bcc2' : '#8c8c8c',
    radiusXs: `${Math.max(4, radius - 8)}px`,
    radiusSm: `${Math.max(8, radius - 4)}px`,
    radiusMd: `${radius}px`,
    radiusLg: `${radius + 2}px`,
    radiusXl: `${radius + 6}px`,
    shadowSm: isDark ? '0 8px 22px rgba(0, 0, 0, 0.24)' : '0 8px 22px rgba(17, 24, 39, 0.05)',
    shadowMd: isDark ? '0 18px 48px rgba(0, 0, 0, 0.34)' : '0 18px 48px rgba(17, 24, 39, 0.08)',
    shadowLg: isDark ? '0 28px 70px rgba(0, 0, 0, 0.42)' : '0 28px 70px rgba(17, 24, 39, 0.12)',
    durationFast: '150ms',
    durationBase: '190ms',
    durationSlow: '220ms',
    densityFactor: input.density === 'compact' ? '0.92' : '1'
  }
}

export function buildCssVariables(tokens: MmDesignTokens) {
  return {
    '--mm-app-bg': tokens.appBg,
    '--mm-surface': tokens.surface,
    '--mm-surface-soft': tokens.surfaceSoft,
    '--mm-surface-muted': tokens.surfaceMuted,
    '--mm-overlay': tokens.overlay,
    '--mm-border': tokens.border,
    '--mm-border-strong': tokens.borderStrong,
    '--mm-focus-ring': tokens.focusRing,
    '--mm-text-primary': tokens.textPrimary,
    '--mm-text-secondary': tokens.textSecondary,
    '--mm-text-muted': tokens.textMuted,
    '--mm-text-disabled': tokens.textDisabled,
    '--mm-text-inverse': tokens.textInverse,
    '--mm-brand-primary': tokens.brandPrimary,
    '--mm-brand-primary-hover': tokens.brandPrimaryHover,
    '--mm-brand-soft': tokens.brandSoft,
    '--mm-brand-border': tokens.brandBorder,
    '--mm-brand-contrast': tokens.brandContrast,
    '--mm-success': tokens.success,
    '--mm-info': tokens.info,
    '--mm-warning': tokens.warning,
    '--mm-danger': tokens.danger,
    '--mm-premium': tokens.premium,
    '--mm-hosted': tokens.hosted,
    '--mm-preview': tokens.preview,
    '--mm-product-mail': tokens.productMail,
    '--mm-product-calendar': tokens.productCalendar,
    '--mm-product-drive': tokens.productDrive,
    '--mm-product-docs': tokens.productDocs,
    '--mm-product-sheets': tokens.productSheets,
    '--mm-product-pass': tokens.productPass,
    '--mm-product-collaboration': tokens.productCollaboration,
    '--mm-product-command': tokens.productCommand,
    '--mm-product-notifications': tokens.productNotifications,
    '--mm-product-admin': tokens.productAdmin,
    '--mm-product-settings': tokens.productSettings,
    '--mm-product-labs': tokens.productLabs,
    '--mm-radius-xs': tokens.radiusXs,
    '--mm-radius-sm': tokens.radiusSm,
    '--mm-radius-md': tokens.radiusMd,
    '--mm-radius-lg': tokens.radiusLg,
    '--mm-radius-xl': tokens.radiusXl,
    '--mm-shadow-sm': tokens.shadowSm,
    '--mm-shadow-md': tokens.shadowMd,
    '--mm-shadow-lg': tokens.shadowLg,
    '--mm-duration-fast': tokens.durationFast,
    '--mm-duration-base': tokens.durationBase,
    '--mm-duration-slow': tokens.durationSlow,
    '--mm-density-factor': tokens.densityFactor,
    '--mm-bg': tokens.appBg,
    '--mm-card': tokens.surface,
    '--mm-card-muted': tokens.surfaceSoft,
    '--mm-ink': tokens.textPrimary,
    '--mm-primary': tokens.brandPrimary,
    '--mm-primary-pressed': tokens.brandPrimaryHover,
    '--mm-accent-soft': tokens.brandSoft,
    '--mm-accent-border': tokens.brandBorder,
    '--mm-admin': tokens.productAdmin,
    '--mm-mail': tokens.productMail,
    '--mm-calendar': tokens.productCalendar,
    '--mm-drive': tokens.productDrive,
    '--mm-docs': tokens.productDocs,
    '--mm-sheets': tokens.productSheets,
    '--mm-pass': tokens.productPass,
    '--mm-governance': tokens.productAdmin,
    '--mm-labs': tokens.productLabs,
    '--mm-e2ee': tokens.success,
    '--mm-security': tokens.success,
    '--mm-side-surface': tokens.surfaceSoft,
    '--mm-topbar': `color-mix(in srgb, ${tokens.surface} 94%, transparent)`,
    '--mm-shadow': tokens.shadowMd,
    '--mm-sheet-density': tokens.densityFactor,
    '--mm-radius': tokens.radiusMd,
    '--mm-text': tokens.textPrimary
  }
}
