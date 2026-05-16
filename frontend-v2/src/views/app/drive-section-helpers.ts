import type { DriveItem } from "@/service/api/drive";

const FILE_SIZE_UNITS = ["B", "KB", "MB", "GB", "TB"];

export function filterDriveItems(items: DriveItem[], key: string) {
  const nextItems = items.slice();

  if (key === "drive-shared") {
    return nextItems.filter((item) => item.shareCount > 0).sort(compareByUpdatedDesc);
  }

  if (key === "drive-recent") {
    return nextItems.sort(compareByUpdatedDesc);
  }

  if (key === "drive-starred") {
    return nextItems.sort((left, right) => {
      const shareDelta = right.shareCount - left.shareCount;
      if (shareDelta !== 0) {
        return shareDelta;
      }

      return compareByUpdatedDesc(left, right);
    });
  }

  if (key === "drive-trash") {
    return nextItems.sort((left, right) => {
      return (
        compareDateDesc(left.createdAt, right.createdAt) || left.name.localeCompare(right.name)
      );
    });
  }

  return nextItems.sort((left, right) => {
    const folderDelta = Number(isFolderItem(left)) - Number(isFolderItem(right));
    if (folderDelta !== 0) {
      return folderDelta * -1;
    }

    return left.name.localeCompare(right.name);
  });
}

export function isFolderItem(item: DriveItem) {
  return item.itemType.toLowerCase().includes("folder");
}

export function formatFileSize(value: number) {
  if (!value || value <= 0) {
    return "0 B";
  }

  let size = value;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < FILE_SIZE_UNITS.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }

  return `${size >= 10 || unitIndex === 0 ? size.toFixed(0) : size.toFixed(1)} ${FILE_SIZE_UNITS[unitIndex]}`;
}

export function formatDateLabel(value: string, fallback: string) {
  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return value || fallback;
  }

  return parsed.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

export function formatDateTime(value: string | null, fallback: string) {
  if (!value) {
    return fallback;
  }

  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}

export function resolveRuntimeError(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}

function compareByUpdatedDesc(left: DriveItem, right: DriveItem) {
  return compareDateDesc(left.updatedAt, right.updatedAt) || left.name.localeCompare(right.name);
}

function compareDateDesc(left: string, right: string) {
  return parseDateValue(right) - parseDateValue(left);
}

function parseDateValue(value: string) {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? 0 : parsed.getTime();
}
