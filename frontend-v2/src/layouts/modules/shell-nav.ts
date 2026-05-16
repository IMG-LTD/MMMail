import { lt, type TextLike } from "@/locales";

type NavTone =
  | "admin"
  | "calendar"
  | "collaboration"
  | "command"
  | "docs"
  | "drive"
  | "labs"
  | "mail"
  | "neutral"
  | "notifications"
  | "pass"
  | "settings"
  | "sheets"
  | "workspace";
type MaturityLevel = "ga" | "beta" | "preview";

export interface NavItem {
  key: string;
  label: TextLike;
  path: string;
  tone: NavTone;
  canonicalPath?: string;
  fallbackPath?: string;
  matchPrefixes?: readonly string[];
  maturity?: MaturityLevel;
  badge?: TextLike;
}

export interface NavGroup {
  title: TextLike;
  caption?: TextLike;
  items: NavItem[];
}

export interface MailNavItem {
  key: string;
  label: TextLike;
  icon: string;
  path: string;
  badge?: TextLike;
}

export interface MailNavSection {
  title: TextLike;
  items: MailNavItem[];
}

export interface MobilePrimaryTab {
  hint: TextLike;
  key: string;
  label: TextLike;
  matchPrefixes: readonly string[];
  path: string;
}

const mailRoutePrefixes = [
  "/archive",
  "/compose",
  "/contacts",
  "/conversations",
  "/drafts",
  "/folders",
  "/inbox",
  "/labels",
  "/outbox",
  "/scheduled",
  "/search",
  "/sent",
  "/snoozed",
  "/spam",
  "/starred",
  "/trash",
  "/unread",
] as const;

export const shellNavGroups: NavGroup[] = [
  {
    title: lt("平台", "平台", "Platform"),
    caption: lt("v2.1 工作台", "v2.1 工作台", "v2.1 workspace"),
    items: [
      {
        key: "workspace",
        label: lt("工作台", "工作台", "Workspace"),
        path: "/suite",
        canonicalPath: "/workspace",
        fallbackPath: "/suite",
        tone: "workspace",
        matchPrefixes: ["/workspace", "/suite"],
      },
    ],
  },
  {
    title: lt("产品", "產品", "Products"),
    items: [
      {
        key: "mail",
        label: lt("邮件", "郵件", "Mail"),
        path: "/inbox",
        canonicalPath: "/mail",
        fallbackPath: "/inbox",
        tone: "mail",
        matchPrefixes: ["/mail", ...mailRoutePrefixes],
      },
      {
        key: "calendar",
        label: lt("日历", "日曆", "Calendar"),
        path: "/calendar",
        canonicalPath: "/calendar",
        fallbackPath: "/calendar",
        tone: "calendar",
        matchPrefixes: ["/calendar"],
      },
      {
        key: "drive",
        label: lt("云盘", "雲端硬碟", "Drive"),
        path: "/drive",
        canonicalPath: "/drive",
        fallbackPath: "/drive",
        tone: "drive",
        matchPrefixes: ["/drive"],
      },
      {
        key: "docs",
        label: lt("文档", "文件", "Docs"),
        path: "/docs",
        canonicalPath: "/docs",
        fallbackPath: "/docs",
        tone: "docs",
        matchPrefixes: ["/docs"],
        maturity: "beta",
      },
      {
        key: "sheets",
        label: lt("表格", "試算表", "Sheets"),
        path: "/sheets",
        canonicalPath: "/sheets",
        fallbackPath: "/sheets",
        tone: "sheets",
        matchPrefixes: ["/sheets"],
        maturity: "beta",
      },
      {
        key: "pass",
        label: lt("密码库", "密碼庫", "Pass"),
        path: "/pass",
        canonicalPath: "/pass",
        fallbackPath: "/pass",
        tone: "pass",
        matchPrefixes: ["/pass", "/pass-monitor"],
      },
    ],
  },
  {
    title: lt("协作与自动化", "協作與自動化", "Collaboration and automation"),
    caption: lt("预览能力", "預覽能力", "Preview capabilities"),
    items: [
      {
        key: "collaboration",
        label: lt("协作", "協作", "Collaboration"),
        path: "/collaboration",
        canonicalPath: "/collaboration",
        fallbackPath: "/collaboration",
        tone: "collaboration",
        matchPrefixes: ["/collaboration"],
        maturity: "preview",
      },
      {
        key: "command-center",
        label: lt("指挥中心", "指揮中心", "Command Center"),
        path: "/command-center",
        canonicalPath: "/command-center",
        fallbackPath: "/command-center",
        tone: "command",
        matchPrefixes: ["/command-center"],
        maturity: "preview",
      },
      {
        key: "notifications",
        label: lt("通知", "通知", "Notifications"),
        path: "/notifications",
        canonicalPath: "/notifications",
        fallbackPath: "/notifications",
        tone: "notifications",
        matchPrefixes: ["/notifications"],
        maturity: "preview",
      },
    ],
  },
  {
    title: lt("治理", "治理", "Governance"),
    items: [
      {
        key: "admin",
        label: lt("管理后台", "管理後台", "Admin"),
        path: "/organizations",
        canonicalPath: "/admin",
        fallbackPath: "/organizations",
        tone: "admin",
        matchPrefixes: ["/admin", "/organizations", "/business", "/security"],
      },
      {
        key: "settings",
        label: lt("设置", "設定", "Settings"),
        path: "/settings",
        canonicalPath: "/settings",
        fallbackPath: "/settings",
        tone: "settings",
        matchPrefixes: ["/settings"],
      },
      {
        key: "labs",
        label: lt("Labs", "Labs", "Labs"),
        path: "/labs",
        canonicalPath: "/labs",
        fallbackPath: "/labs",
        tone: "labs",
        matchPrefixes: ["/labs"],
        maturity: "preview",
      },
    ],
  },
];

export const mailNavSections: MailNavSection[] = [
  {
    title: lt("工作区", "工作區", "Workspace"),
    items: [
      {
        key: "inbox",
        label: lt("收件箱", "收件匣", "Inbox"),
        icon: "▣",
        path: "/inbox",
        badge: "248",
      },
      { key: "contacts", label: lt("联系人", "聯絡人", "Contacts"), icon: "◫", path: "/contacts" },
      { key: "categories", label: lt("搜索", "搜尋", "Search"), icon: "⤢", path: "/search" },
    ],
  },
  {
    title: lt("文件夹", "資料夾", "Folders"),
    items: [
      {
        key: "all-mail",
        label: lt("星标", "已加星號", "Starred"),
        icon: "⌂",
        path: "/starred",
        badge: "19",
      },
      { key: "sent", label: lt("已发送", "已傳送", "Sent"), icon: "↗", path: "/sent" },
      {
        key: "drafts",
        label: lt("草稿", "草稿", "Drafts"),
        icon: "◧",
        path: "/drafts",
        badge: "3",
      },
      { key: "archive", label: lt("归档", "封存", "Archive"), icon: "◫", path: "/archive" },
      {
        key: "spam",
        label: lt("垃圾邮件", "垃圾郵件", "Spam"),
        icon: "!",
        path: "/spam",
        badge: "1",
      },
      { key: "trash", label: lt("废纸篓", "垃圾桶", "Trash"), icon: "✕", path: "/trash" },
    ],
  },
  {
    title: lt("标签", "標籤", "Labels"),
    items: [
      { key: "urgent", label: lt("紧急", "緊急", "Urgent"), icon: "●", path: "/labels/urgent" },
      {
        key: "projects",
        label: lt("项目", "專案", "Projects"),
        icon: "●",
        path: "/labels/projects",
      },
    ],
  },
];

export const mobilePrimaryTabs: MobilePrimaryTab[] = [
  {
    key: "workspace",
    label: lt("工作台", "工作台", "Home"),
    hint: lt("总览", "總覽", "Overview"),
    path: "/suite",
    matchPrefixes: ["/workspace", "/suite"],
  },
  {
    key: "mail",
    label: lt("邮件", "郵件", "Mail"),
    hint: lt("收件箱", "收件匣", "Inbox"),
    path: "/inbox",
    matchPrefixes: ["/mail", ...mailRoutePrefixes],
  },
  {
    key: "calendar",
    label: lt("日历", "日曆", "Calendar"),
    hint: lt("议程", "議程", "Agenda"),
    path: "/calendar",
    matchPrefixes: ["/calendar"],
  },
  {
    key: "drive",
    label: lt("云盘", "雲端硬碟", "Drive"),
    hint: lt("文件", "檔案", "Files"),
    path: "/drive",
    matchPrefixes: ["/drive"],
  },
  {
    key: "more",
    label: lt("更多", "更多", "More"),
    hint: lt("更多模块", "更多模組", "More"),
    path: "/suite",
    matchPrefixes: [
      "/docs",
      "/sheets",
      "/pass",
      "/collaboration",
      "/command-center",
      "/notifications",
      "/admin",
      "/organizations",
      "/business",
      "/security",
      "/settings",
      "/labs",
    ],
  },
];

export function isRouteMatch(
  path: string,
  item: Pick<NavItem, "path" | "matchPrefixes"> | MobilePrimaryTab,
) {
  if (item.matchPrefixes?.length) {
    return item.matchPrefixes.some((prefix) => path.startsWith(prefix));
  }

  return path === item.path;
}

export function isMailRoute(path: string) {
  return mailRoutePrefixes.some((prefix) => path.startsWith(prefix)) || path.startsWith("/mail");
}

export function findShellSurface(path: string) {
  for (const group of shellNavGroups) {
    const match = group.items.find((item) => isRouteMatch(path, item));

    if (match) {
      return match;
    }
  }

  return null;
}

export function getToneColorVar(tone: NavTone) {
  if (tone === "workspace") return "var(--mm-brand-primary)";
  if (tone === "mail") return "var(--mm-product-mail)";
  if (tone === "calendar") return "var(--mm-product-calendar)";
  if (tone === "docs") return "var(--mm-product-docs)";
  if (tone === "sheets") return "var(--mm-product-sheets)";
  if (tone === "pass") return "var(--mm-product-pass)";
  if (tone === "labs") return "var(--mm-product-labs)";
  if (tone === "drive") return "var(--mm-product-drive)";
  if (tone === "collaboration") return "var(--mm-product-collaboration)";
  if (tone === "command") return "var(--mm-product-command)";
  if (tone === "notifications") return "var(--mm-product-notifications)";
  if (tone === "admin") return "var(--mm-product-admin)";
  if (tone === "settings") return "var(--mm-product-settings)";
  return "var(--mm-product-admin)";
}
