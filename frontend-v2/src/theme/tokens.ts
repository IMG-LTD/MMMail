import type { GlobalThemeOverrides } from 'naive-ui'
import { resolveThemePreset, type ResolvedThemeScheme, type ThemeDensity, type ThemePresetId } from './settings'

interface ThemeInput {
  density: ThemeDensity
  preset: ThemePresetId
  radius: number
  scheme: ResolvedThemeScheme
}

interface ThemeModel {
  cssVars: Record<string, string>
  naiveThemeOverrides: GlobalThemeOverrides
}

interface SurfacePalette {
  bg: string
  border: string
  borderStrong: string
  card: string
  cardMuted: string
  danger: string
  ink: string
  shadow: string
  sideSurface: string
  text: string
  textSecondary: string
  topbar: string
  warning: string
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

function getModulePalette(scheme: ResolvedThemeScheme) {
  if (scheme === 'dark') {
    return {
      admin: '#9aa1ae',
      calendar: '#5dc9a1',
      docs: '#67b2e3',
      drive: '#a68dfb',
      labs: '#b8bcc2',
      mail: '#8a9cf4',
      pass: '#f2a574',
      security: '#3ab890',
      sheets: '#6bc477'
    }
  }

  return {
    admin: '#535b6a',
    calendar: '#2fa37b',
    docs: '#2e8bcc',
    drive: '#7a5af8',
    labs: '#8c8c8c',
    mail: '#4f6bed',
    pass: '#e47b39',
    security: '#138a6b',
    sheets: '#3fa04e'
  }
}

function getSurfacePalette(scheme: ResolvedThemeScheme): SurfacePalette {
  if (scheme === 'dark') {
    return {
      bg: '#0e1014',
      border: '#242833',
      borderStrong: '#303646',
      card: '#151820',
      cardMuted: '#10141b',
      danger: '#e97a72',
      ink: '#e6e8ec',
      shadow: '0 22px 56px rgba(0, 0, 0, 0.34)',
      sideSurface: '#121620',
      text: '#e6e8ec',
      textSecondary: '#c0c6ce',
      topbar: 'rgba(14, 16, 20, 0.92)',
      warning: '#e8b547'
    }
  }

  return {
    bg: '#ffffff',
    border: '#e3e5ea',
    borderStrong: '#d6dbe4',
    card: '#ffffff',
    cardMuted: '#f6f7f9',
    danger: '#d0483f',
    ink: '#111318',
    shadow: '0 18px 48px rgba(17, 19, 24, 0.06)',
    sideSurface: '#f8f9fb',
    text: '#1b1d22',
    textSecondary: '#3a3f48',
    topbar: 'rgba(255, 255, 255, 0.94)',
    warning: '#d48c00'
  }
}

export function applyThemeVariables(cssVars: Record<string, string>) {
  Object.entries(cssVars).forEach(([key, value]) => {
    document.documentElement.style.setProperty(key, value)
  })
}

export function buildMmMailTheme(input: ThemeInput): ThemeModel {
  const preset = resolveThemePreset(input.preset)
  const surface = getSurfacePalette(input.scheme)
  const modules = getModulePalette(input.scheme)
  const accentSoft = withAlpha(preset.accent, input.scheme === 'dark' ? 0.2 : 0.11)
  const accentBorder = withAlpha(preset.accent, input.scheme === 'dark' ? 0.44 : 0.22)
  const densityFactor = input.density === 'compact' ? 0.92 : 1

  return {
    cssVars: {
      '--mm-accent-border': accentBorder,
      '--mm-accent-soft': accentSoft,
      '--mm-admin': modules.admin,
      '--mm-bg': surface.bg,
      '--mm-border': surface.border,
      '--mm-border-strong': surface.borderStrong,
      '--mm-calendar': modules.calendar,
      '--mm-card': surface.card,
      '--mm-card-muted': surface.cardMuted,
      '--mm-danger': surface.danger,
      '--mm-docs': modules.docs,
      '--mm-drive': modules.drive,
      '--mm-e2ee': modules.security,
      '--mm-governance': modules.admin,
      '--mm-ink': surface.ink,
      '--mm-labs': modules.labs,
      '--mm-mail': modules.mail,
      '--mm-pass': modules.pass,
      '--mm-primary': preset.accent,
      '--mm-primary-pressed': preset.accentPressed,
      '--mm-radius': `${input.radius}px`,
      '--mm-security': modules.security,
      '--mm-shadow': surface.shadow,
      '--mm-sheet-density': densityFactor.toString(),
      '--mm-sheets': modules.sheets,
      '--mm-side-surface': surface.sideSurface,
      '--mm-text': surface.text,
      '--mm-text-secondary': surface.textSecondary,
      '--mm-topbar': surface.topbar,
      '--mm-warning': surface.warning
    },
    naiveThemeOverrides: {
      common: {
        bodyColor: surface.bg,
        borderColor: surface.border,
        borderRadius: `${input.radius}px`,
        cardColor: surface.cardMuted,
        errorColor: surface.danger,
        fontFamily: '"Public Sans", "Segoe UI", sans-serif',
        infoColor: modules.mail,
        modalColor: surface.card,
        popoverColor: surface.card,
        primaryColor: preset.accent,
        primaryColorHover: preset.accent,
        primaryColorPressed: preset.accentPressed,
        successColor: modules.security,
        tableColor: surface.card,
        textColor1: surface.ink,
        textColor2: surface.textSecondary,
        textColorBase: surface.text,
        warningColor: surface.warning
      },
      Button: {
        borderRadiusLarge: `${input.radius}px`,
        borderRadiusMedium: `${input.radius}px`,
        borderRadiusSmall: `${Math.max(8, input.radius - 2)}px`,
        heightLarge: input.density === 'compact' ? '38px' : '42px',
        heightMedium: input.density === 'compact' ? '32px' : '36px'
      },
      Drawer: {
        color: surface.card
      },
      Input: {
        borderFocus: preset.accent,
        borderHover: preset.accent,
        color: surface.cardMuted
      }
    }
  }
}
