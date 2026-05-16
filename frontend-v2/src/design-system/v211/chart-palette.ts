import type { Tone } from "./types";

export const v211ChartPalette = [
  "#168a66",
  "#2563eb",
  "#d97706",
  "#8b5cf6",
  "#db2777",
  "#475467",
] as const;

export const v211ToneColors: Record<Tone, string> = {
  brand: "#168a66",
  danger: "#d92d20",
  info: "#2563eb",
  neutral: "#667085",
  success: "#168a66",
  warning: "#d97706",
};
