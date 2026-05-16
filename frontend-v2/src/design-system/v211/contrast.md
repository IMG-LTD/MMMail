# v2.1.1 Contrast Record

This record verifies the v2.1.1 semantic color pairs used by `tokens.css`, `theme.ts`, and the Naive UI theme bridge. Ratios were calculated with WCAG relative luminance on May 15, 2026.

| Foreground token | Background token | Pair                   | Ratio   | Use                      |
| ---------------- | ---------------- | ---------------------- | ------- | ------------------------ |
| `textPrimary`    | `surface`        | `#111827` on `#ffffff` | 17.74:1 | Body text and headings   |
| `textSecondary`  | `surface`        | `#3f4652` on `#ffffff` | 9.51:1  | Secondary copy           |
| `textMuted`      | `surface`        | `#667085` on `#ffffff` | 4.97:1  | Metadata and helper text |
| `brandContrast`  | `brandPrimary`   | `#ffffff` on `#4f6bed` | 4.51:1  | Primary filled buttons   |
| `brandPrimary`   | `surface`        | `#4f6bed` on `#ffffff` | 4.51:1  | Links and selected text  |
| `danger`         | `surface`        | `#d92d20` on `#ffffff` | 4.83:1  | Destructive labels       |
| `info`           | `surface`        | `#2563eb` on `#ffffff` | 5.17:1  | Informational labels     |
| `textPrimary`    | `warning`        | `#111827` on `#d97706` | 5.57:1  | Warning filled tags      |
| `textInverse`    | `productAdmin`   | `#ffffff` on `#535b6a` | 6.84:1  | Admin filled tags        |
| `textInverse`    | `productCommand` | `#ffffff` on `#475467` | 7.69:1  | Command filled tags      |

Accent-only tokens below 4.5:1 against `surface` (`success`, `hosted`, `premium`) are not used as standalone body text. They are used as icons, borders, soft backgrounds, or Naive UI tags with adjacent textual labels, so color is not the only carrier of meaning.
