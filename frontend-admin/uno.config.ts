import { defineConfig, transformerDirectives, transformerVariantGroup, presetWind3 } from 'unocss';
import { presetSoybeanAdmin } from '@sa/uno-preset';
import { themeVars } from './src/theme/vars';

export default defineConfig({
  content: {
    pipeline: {
      exclude: ['node_modules', 'dist']
    }
  },
  theme: {
    ...themeVars,
    colors: {
      ...themeVars.colors,
      'module-mail': '#4C6EF5',
      'module-calendar': '#2D9D8F',
      'module-drive': '#12B886',
      'module-docs': '#228BE6',
      'module-sheets': '#40C057',
      'module-pass': '#7950F2',
      'module-admin': '#E67700',
      'module-settings': '#5C7CFA'
    },
    fontSize: {
      'icon-xs': '0.875rem',
      'icon-small': '1rem',
      icon: '1.125rem',
      'icon-large': '1.5rem',
      'icon-xl': '2rem'
    }
  },
  shortcuts: {
    'card-wrapper': 'rd-8px shadow-sm'
  },
  transformers: [transformerDirectives(), transformerVariantGroup()],
  presets: [presetWind3({ dark: 'class' }), presetSoybeanAdmin()]
});
