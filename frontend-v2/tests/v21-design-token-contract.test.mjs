import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const files = {
  designTokens: new URL("../src/design-system/tokens.ts", import.meta.url),
  naiveTheme: new URL("../src/design-system/naive-theme.ts", import.meta.url),
  themeTokens: new URL("../src/theme/tokens.ts", import.meta.url),
  globalCss: new URL("../src/styles/global.css", import.meta.url),
};

const requiredSemanticVars = {
  "--mm-app-bg": "appBg",
  "--mm-surface": "surface",
  "--mm-surface-soft": "surfaceSoft",
  "--mm-surface-muted": "surfaceMuted",
  "--mm-overlay": "overlay",
  "--mm-border": "border",
  "--mm-border-strong": "borderStrong",
  "--mm-focus-ring": "focusRing",
  "--mm-text-primary": "textPrimary",
  "--mm-text-secondary": "textSecondary",
  "--mm-text-muted": "textMuted",
  "--mm-text-disabled": "textDisabled",
  "--mm-text-inverse": "textInverse",
  "--mm-brand-primary": "brandPrimary",
  "--mm-brand-primary-hover": "brandPrimaryHover",
  "--mm-brand-soft": "brandSoft",
  "--mm-brand-border": "brandBorder",
  "--mm-brand-contrast": "brandContrast",
  "--mm-success": "success",
  "--mm-info": "info",
  "--mm-warning": "warning",
  "--mm-danger": "danger",
  "--mm-premium": "premium",
  "--mm-hosted": "hosted",
  "--mm-preview": "preview",
  "--mm-product-mail": "productMail",
  "--mm-product-calendar": "productCalendar",
  "--mm-product-drive": "productDrive",
  "--mm-product-docs": "productDocs",
  "--mm-product-sheets": "productSheets",
  "--mm-product-pass": "productPass",
  "--mm-product-collaboration": "productCollaboration",
  "--mm-product-command": "productCommand",
  "--mm-product-notifications": "productNotifications",
  "--mm-product-admin": "productAdmin",
  "--mm-product-settings": "productSettings",
  "--mm-product-labs": "productLabs",
  "--mm-radius-xs": "radiusXs",
  "--mm-radius-sm": "radiusSm",
  "--mm-radius-md": "radiusMd",
  "--mm-radius-lg": "radiusLg",
  "--mm-radius-xl": "radiusXl",
  "--mm-shadow-sm": "shadowSm",
  "--mm-shadow-md": "shadowMd",
  "--mm-shadow-lg": "shadowLg",
  "--mm-duration-fast": "durationFast",
  "--mm-duration-base": "durationBase",
  "--mm-duration-slow": "durationSlow",
  "--mm-density-factor": "densityFactor",
};

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

test("v2.1 semantic tokens are declared and exported", async () => {
  const [designTokens, naiveTheme, themeTokens, globalCss] = await Promise.all(
    Object.values(files).map((file) => readFile(file, "utf8")),
  );

  assert.match(designTokens, /export interface MmDesignTokens/);
  assert.match(designTokens, /export function buildMmDesignTokens/);
  assert.match(designTokens, /export function buildCssVariables/);
  assert.match(naiveTheme, /export function buildNaiveThemeOverrides/);
  assert.match(themeTokens, /buildMmDesignTokens/);
  assert.match(themeTokens, /buildV211ThemeOverrides/);

  for (const [variable, property] of Object.entries(requiredSemanticVars)) {
    assert.match(globalCss, new RegExp(escapeRegExp(variable)));
    assert.match(designTokens, new RegExp(`\\b${property}: string`));
    assert.match(designTokens, new RegExp(`'${escapeRegExp(variable)}': tokens\\.${property}`));
  }

  assert.match(naiveTheme, /const isCompact = Number\(tokens\.densityFactor\) < 1/);
  assert.match(naiveTheme, /heightLarge: isCompact \? '38px' : '42px'/);
  assert.match(naiveTheme, /heightMedium: isCompact \? '32px' : '36px'/);
});
