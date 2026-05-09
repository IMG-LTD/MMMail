import type { GlobalThemeOverrides } from 'naive-ui'
import type { MmDesignTokens } from './tokens'

export function buildNaiveThemeOverrides(tokens: MmDesignTokens): GlobalThemeOverrides {
  const isCompact = Number(tokens.densityFactor) < 1

  return {
    common: {
      bodyColor: tokens.appBg,
      borderColor: tokens.border,
      borderRadius: tokens.radiusMd,
      cardColor: tokens.surface,
      errorColor: tokens.danger,
      fontFamily: '"Public Sans", "Segoe UI", sans-serif',
      infoColor: tokens.info,
      modalColor: tokens.surface,
      popoverColor: tokens.surface,
      primaryColor: tokens.brandPrimary,
      primaryColorHover: tokens.brandPrimaryHover,
      primaryColorPressed: tokens.brandPrimaryHover,
      successColor: tokens.success,
      tableColor: tokens.surface,
      textColor1: tokens.textPrimary,
      textColor2: tokens.textSecondary,
      textColor3: tokens.textMuted,
      textColorBase: tokens.textPrimary,
      warningColor: tokens.warning
    },
    Button: {
      borderRadiusLarge: tokens.radiusMd,
      borderRadiusMedium: tokens.radiusSm,
      borderRadiusSmall: tokens.radiusXs,
      heightLarge: isCompact ? '38px' : '42px',
      heightMedium: isCompact ? '32px' : '36px'
    },
    Card: {
      borderRadius: tokens.radiusLg,
      color: tokens.surface
    },
    Drawer: {
      color: tokens.surface
    },
    Input: {
      borderFocus: tokens.brandPrimary,
      borderHover: tokens.brandPrimary,
      color: tokens.surfaceSoft
    },
    Modal: {
      borderRadius: tokens.radiusLg,
      color: tokens.surface
    },
    Tabs: {
      tabTextColorActiveLine: tokens.brandPrimary,
      barColor: tokens.brandPrimary
    }
  }
}
