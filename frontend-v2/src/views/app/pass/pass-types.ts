import type { PassMailbox, PassMonitorItem, PassWorkspaceItemSummary } from "@/service/api/pass";

export interface PassSurfaceEntry {
  avatar: string;
  badge: string;
  id: string;
  item?: PassWorkspaceItemSummary;
  key: string;
  mailbox?: PassMailbox;
  meta: string;
  monitorItem?: PassMonitorItem;
  subtitle: string;
  title: string;
}

export interface PassCardFact {
  label: string;
  value: string;
}

export interface PassDetailCard {
  copy: string;
  facts: PassCardFact[];
  label: string;
  title: string;
}

export type PassConfirmAction = "rotate" | "revoke" | "delete";
