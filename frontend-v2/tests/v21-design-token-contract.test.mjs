import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  designTokens: new URL('../src/design-system/tokens.ts', import.meta.url),
  naiveTheme: new URL('../src/design-system/naive-theme.ts', import.meta.url),
  themeTokens: new URL('../src/theme/tokens.ts', import.meta.url),
  globalCss: new URL('../src/styles/global.css', import.meta.url)
}

const requiredCssVars = [
  '--mm-app-bg',
  '--mm-surface',
  '--mm-surface-soft',
  '--mm-surface-muted',
  '--mm-overlay',
  '--mm-border',
  '--mm-border-strong',
  '--mm-focus-ring',
  '--mm-text-primary',
  '--mm-text-secondary',
  '--mm-text-muted',
  '--mm-text-disabled',
  '--mm-text-inverse',
  '--mm-brand-primary',
  '--mm-brand-primary-hover',
  '--mm-brand-soft',
  '--mm-brand-border',
  '--mm-brand-contrast',
  '--mm-success',
  '--mm-info',
  '--mm-warning',
  '--mm-danger',
  '--mm-premium',
  '--mm-hosted',
  '--mm-preview',
  '--mm-product-mail',
  '--mm-product-calendar',
  '--mm-product-drive',
  '--mm-product-docs',
  '--mm-product-sheets',
  '--mm-product-pass',
  '--mm-product-collaboration',
  '--mm-product-command',
  '--mm-product-notifications',
  '--mm-product-admin',
  '--mm-product-settings',
  '--mm-product-labs',
  '--mm-radius-xs',
  '--mm-radius-sm',
  '--mm-radius-md',
  '--mm-radius-lg',
  '--mm-radius-xl',
  '--mm-shadow-sm',
  '--mm-shadow-md',
  '--mm-shadow-lg',
  '--mm-duration-fast',
  '--mm-duration-base',
  '--mm-duration-slow'
]

test('v2.1 semantic tokens are declared and exported', async () => {
  const [designTokens, naiveTheme, themeTokens, globalCss] = await Promise.all(
    Object.values(files).map(file => readFile(file, 'utf8'))
  )

  assert.match(designTokens, /export interface MmDesignTokens/)
  assert.match(designTokens, /export function buildMmDesignTokens/)
  assert.match(designTokens, /export function buildCssVariables/)
  assert.match(naiveTheme, /export function buildNaiveThemeOverrides/)
  assert.match(themeTokens, /buildMmDesignTokens/)
  assert.match(themeTokens, /buildNaiveThemeOverrides/)

  for (const variable of requiredCssVars) {
    assert.match(globalCss, new RegExp(variable.replaceAll('-', '\\-')))
    assert.match(designTokens, new RegExp(variable.replace('--mm-', '').replaceAll('-', '[A-Z][a-z]+|[a-z]+')))
  }
})
