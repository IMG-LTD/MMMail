import type { TextLike } from "@/locales";

export type { TextLike };

export const V211_MODULE_IDS = [
  "home",
  "mail",
  "calendar",
  "drive",
  "docs",
  "sheets",
  "pass",
  "collaboration",
  "command-center",
  "notifications",
  "admin",
  "settings",
] as const;

export type ModuleId = (typeof V211_MODULE_IDS)[number];
export type Tone = "success" | "warning" | "danger" | "info" | "neutral" | "brand";

export interface AsyncState {
  readonly loading?: boolean;
  readonly error?: string | null;
  readonly empty?: boolean;
}

export interface StatusTag {
  readonly id?: string;
  readonly label: TextLike;
  readonly tone?: Tone;
}

export interface SectionAction {
  readonly id: string;
  readonly label: TextLike;
  readonly tone?: Tone;
  readonly disabled?: boolean;
}

export interface FilterConfig {
  readonly id: string;
  readonly label: TextLike;
  readonly options: readonly {
    readonly label: TextLike;
    readonly value: string | number | boolean;
  }[];
}

export interface BulkAction {
  readonly id: string;
  readonly label: TextLike;
  readonly tone?: Tone;
  readonly disabled?: boolean;
}

export interface ItemAction {
  readonly id: string;
  readonly label: TextLike;
  readonly tone?: Tone;
  readonly disabled?: boolean;
}

export interface EntityListItem {
  readonly id: string;
  readonly avatar?: string;
  readonly title: TextLike;
  readonly meta?: TextLike;
  readonly tags?: readonly StatusTag[];
  readonly badge?: TextLike | number;
  readonly actions?: readonly ItemAction[];
  readonly disabled?: boolean;
  readonly ariaLabel?: TextLike;
}

export interface InsightTab {
  readonly id: string;
  readonly label: TextLike;
  readonly badge?: number | string;
  readonly disabled?: boolean;
}

export interface ModuleToolbarTab {
  readonly id: string;
  readonly label: TextLike;
  readonly disabled?: boolean;
}

export interface ModuleToolbarAction {
  readonly id: string;
  readonly label: TextLike;
  readonly tone?: Tone;
  readonly disabled?: boolean;
}

export interface BreadcrumbItem {
  readonly label: TextLike;
  readonly path?: string;
}

export interface ChartDatum {
  readonly name: string;
  readonly value: number;
}

export type ChartKind = "line" | "bar" | "pie" | "gauge";

export interface V211ChartOption {
  readonly type: ChartKind;
  readonly data: readonly ChartDatum[];
  readonly title?: TextLike;
}
