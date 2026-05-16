import type { GlobalThemeOverrides } from "naive-ui";
import { buildCssVariables, buildMmDesignTokens } from "@/design-system/tokens";
import { buildV211ThemeOverrides } from "@/design-system/v211/theme";
import type { ResolvedThemeScheme, ThemeDensity, ThemePresetId } from "./settings";

interface ThemeInput {
  density: ThemeDensity;
  preset: ThemePresetId;
  radius: number;
  scheme: ResolvedThemeScheme;
}

interface ThemeModel {
  cssVars: Record<string, string>;
  naiveThemeOverrides: GlobalThemeOverrides;
}

export function applyThemeVariables(cssVars: Record<string, string>) {
  Object.entries(cssVars).forEach(([key, value]) => {
    document.documentElement.style.setProperty(key, value);
  });
}

export function buildMmMailTheme(input: ThemeInput): ThemeModel {
  const tokens = buildMmDesignTokens(input);

  return {
    cssVars: buildCssVariables(tokens),
    naiveThemeOverrides: buildV211ThemeOverrides(tokens),
  };
}
