import type { GlobalThemeOverrides } from "naive-ui";
import { buildNaiveThemeOverrides } from "@/design-system/naive-theme";
import type { MmDesignTokens } from "@/design-system/tokens";

export const v211ThemeTokenKeys = [
  "brandPrimary",
  "surface",
  "textPrimary",
  "textSecondary",
  "focusRing",
] as const;

export function buildV211ThemeOverrides(tokens: MmDesignTokens): GlobalThemeOverrides {
  return buildNaiveThemeOverrides(tokens);
}

export function resolveV211ThemeOverrides(overrides: GlobalThemeOverrides): GlobalThemeOverrides {
  return overrides;
}
