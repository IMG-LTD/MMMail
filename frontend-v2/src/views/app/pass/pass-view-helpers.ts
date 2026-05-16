import type { PassMailbox, PassMonitorItem, PassWorkspaceItemSummary } from "@/service/api/pass";
import { isPassItemShared, isPassMailboxVerified } from "../pass-section-state";

export function compareItemsForVault(
  left: PassWorkspaceItemSummary,
  right: PassWorkspaceItemSummary,
) {
  return Number(right.favorite) - Number(left.favorite) || compareItemsByUpdated(left, right);
}

export function compareItemsForSecureLinks(
  left: PassWorkspaceItemSummary,
  right: PassWorkspaceItemSummary,
) {
  return right.secureLinkCount - left.secureLinkCount || compareItemsByUpdated(left, right);
}

export function compareMonitorItems(left: PassMonitorItem, right: PassMonitorItem) {
  return (
    countMonitorSignals(right) - countMonitorSignals(left) ||
    compareDateDesc(left.updatedAt, right.updatedAt)
  );
}

export function compareMailboxes(left: PassMailbox, right: PassMailbox) {
  return (
    Number(right.defaultMailbox) - Number(left.defaultMailbox) ||
    Number(right.primaryMailbox) - Number(left.primaryMailbox) ||
    Number(isPassMailboxVerified(right)) - Number(isPassMailboxVerified(left)) ||
    compareDateDesc(left.updatedAt, right.updatedAt)
  );
}

export function compareItemsByUpdated(
  left: Pick<PassWorkspaceItemSummary, "title" | "updatedAt">,
  right: Pick<PassWorkspaceItemSummary, "title" | "updatedAt">,
) {
  return (
    compareDateDesc(left.updatedAt, right.updatedAt) ||
    String(left.title || "").localeCompare(String(right.title || ""))
  );
}

export function countMonitorSignals(item: PassMonitorItem) {
  return (
    Number(item.weakPassword) +
    Number(item.reusedPassword) +
    Number(item.inactiveTwoFactor) +
    Number(item.excluded)
  );
}

export function resolvePassItemType(
  itemType: PassWorkspaceItemSummary["itemType"] | PassMonitorItem["itemType"],
) {
  const names = {
    ALIAS: "Alias",
    CARD: "Card",
    LOGIN: "Login",
    NOTE: "Secure note",
    PASSKEY: "Passkey",
    PASSWORD: "Password",
  };
  return names[itemType] || itemType;
}

export function resolvePassScope(
  scopeType: PassWorkspaceItemSummary["scopeType"] | PassMonitorItem["scopeType"],
) {
  return scopeType === "SHARED" ? "Shared" : "Personal";
}

export function resolveMonitorFlags(item: PassMonitorItem) {
  return joinText([
    item.weakPassword ? "Weak password" : null,
    item.reusedPassword ? "Reused password" : null,
    item.inactiveTwoFactor ? "2FA inactive" : null,
    item.excluded ? "Excluded" : null,
  ]);
}

export function resolveMailboxStatus(mailbox: PassMailbox) {
  return isPassMailboxVerified(mailbox) ? "Verified" : "Pending verification";
}

export function createAvatar(value: string) {
  const match = value.trim().match(/[\p{L}\p{N}]/u);
  return (match?.[0] || "#").toUpperCase();
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) return "Not set";
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString();
}

export function joinText(parts: Array<string | null | undefined>) {
  return parts.filter((value): value is string => Boolean(value && value.trim())).join(" · ");
}

export function isSharedItem(item: PassWorkspaceItemSummary) {
  return isPassItemShared(item);
}

function compareDateDesc(left: string | null | undefined, right: string | null | undefined) {
  return parseDateValue(right) - parseDateValue(left);
}

function parseDateValue(value: string | null | undefined) {
  if (!value) return 0;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? 0 : parsed.getTime();
}
