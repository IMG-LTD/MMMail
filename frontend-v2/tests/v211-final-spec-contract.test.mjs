import test from "node:test";
import assert from "node:assert/strict";
import { access, readFile, readdir } from "node:fs/promises";
import { constants } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.resolve(fileURLToPath(new URL("..", import.meta.url)));
const repoDir = path.resolve(rootDir, "..");
const srcDir = path.join(rootDir, "src");
const v211Dir = path.join(srcDir, "design-system", "v211");
const fixturesDir = path.join(rootDir, "tests", "fixtures");

const finalDesignSources = [
  ["home", "/", "docs/MMMail/UI/首页/工作台-设计概览.png"],
  ["mail", "/mail", "docs/MMMail/UI/邮件/邮件-设计概览.png"],
  ["calendar", "/calendar", "docs/MMMail/UI/日历/日历概览.png"],
  ["drive", "/drive", "docs/MMMail/UI/云盘/云盘概览.png"],
  ["docs", "/docs", "docs/MMMail/UI/文档/文档概览.png"],
  ["sheets", "/sheets", "docs/MMMail/UI/Sheets和labs/表格概览.png"],
  ["pass", "/pass", "docs/MMMail/UI/Pass/Pass概览.png"],
  ["collaboration", "/collaboration", "docs/MMMail/UI/Collaboration/协作概览.png"],
  ["command-center", "/command-center", "docs/MMMail/UI/CommandCenter/命令概览.png"],
  ["notifications", "/notifications", "docs/MMMail/UI/Notifications/通知概览.png"],
  ["admin", "/admin", "docs/MMMail/UI/Admin/管理后台.png"],
  ["settings", "/settings", "docs/MMMail/UI/Setting/设置概览.png"],
];

const requiredV211Files = [
  "index.ts",
  "types.ts",
  "tokens.css",
  "theme.ts",
  "branding.ts",
  "breakpoints.ts",
  "format.ts",
  "chart-palette.ts",
  "contrast.md",
  "chart/index.ts",
  "chart/runtime.ts",
  "chart/V211Chart.vue",
  "chart/V211MiniChart.vue",
  "components/V211AppShell.vue",
  "components/V211ModuleToolbar.vue",
  "components/V211DataToolbar.vue",
  "components/V211SectionPanel.vue",
  "components/V211MetricCard.vue",
  "components/V211EntityList.vue",
  "components/V211RightInsightPanel.vue",
  "components/V211ActionBar.vue",
  "components/V211StatusTag.vue",
];

const requiredV211Exports = [
  "V211AppShell",
  "V211ModuleToolbar",
  "V211DataToolbar",
  "V211SectionPanel",
  "V211MetricCard",
  "V211EntityList",
  "V211RightInsightPanel",
  "V211ActionBar",
  "V211StatusTag",
  "V211Chart",
  "V211MiniChart",
  "buildV211ThemeOverrides",
  "V211_MODULE_IDS",
  "formatV211Number",
  "formatV211DateTime",
  "formatV211Bytes",
];

const requiredComponents = [
  [
    "components/V211AppShell.vue",
    [
      "NLayout",
      "NLayoutSider",
      "NLayoutHeader",
      "NLayoutContent",
      "NMenu",
      "NDropdown",
      "NAvatar",
      "NBadge",
    ],
  ],
  ["components/V211ModuleToolbar.vue", ["NPageHeader", "NBreadcrumb", "NTabs", "NButton"]],
  ["components/V211DataToolbar.vue", ["NInput", "NButton", "NDropdown", "NSpace", "NBadge"]],
  [
    "components/V211SectionPanel.vue",
    ["NCard", "NButton", "NSkeleton", "NEmpty", "NResult", "NCollapse"],
  ],
  [
    "components/V211MetricCard.vue",
    ["NCard", "NStatistic", "NNumberAnimation", "NProgress", "NTag"],
  ],
  [
    "components/V211EntityList.vue",
    ["NList", "NThing", "NAvatar", "NTag", "NCheckbox", "NVirtualList"],
  ],
  [
    "components/V211RightInsightPanel.vue",
    ["NCard", "NTabs", "NScrollbar", "NSkeleton", "NEmpty", "NResult"],
  ],
  ["components/V211ActionBar.vue", ["NButton", "NPopconfirm", "NSpace", "NBadge"]],
  ["components/V211StatusTag.vue", ["NTag"]],
];

const nativeTags = ["button", "input", "textarea", "select", "table"];
const allowedNativeReasons = new Set([
  "spreadsheet-cell-editor",
  "calendar-time-grid",
  "rich-text-editor-canvas",
  "chart-rendering-canvas",
]);

function relativeToRepo(filePath) {
  return path.relative(repoDir, filePath).split(path.sep).join("/");
}

async function assertReadable(filePath) {
  await access(filePath, constants.R_OK);
}

async function readJson(filePath) {
  return JSON.parse(await readFile(filePath, "utf8"));
}

async function collectFiles(dir, predicate) {
  const entries = await readdir(dir, { withFileTypes: true });
  const nested = await Promise.all(
    entries.map(async (entry) => {
      const fullPath = path.join(dir, entry.name);

      if (entry.isDirectory()) {
        return collectFiles(fullPath, predicate);
      }

      return predicate(fullPath) ? [fullPath] : [];
    }),
  );

  return nested.flat();
}

function findNativeControls(content) {
  return nativeTags.flatMap((tag) => {
    const pattern = new RegExp(`<\\s*${tag}\\b`, "gi");
    const matches = content.match(pattern) ?? [];
    return matches.map(() => tag);
  });
}

test("v2.1.1 declares every final design source and verifies image availability", async () => {
  const fixturePath = path.join(fixturesDir, "v211-design-source.json");
  const fixture = await readJson(fixturePath);

  for (const [moduleId, routeRoot, designSource] of finalDesignSources) {
    assert.deepEqual(fixture[moduleId], { routeRoot, designSource });
    await assertReadable(path.join(repoDir, designSource));
  }
});

test("v2.1.1 exposes the final design-system directory, barrel, and Naive component contracts", async () => {
  const barrel = await readFile(path.join(v211Dir, "index.ts"), "utf8");

  for (const relativePath of requiredV211Files) {
    await assertReadable(path.join(v211Dir, relativePath));
  }

  for (const exportName of requiredV211Exports) {
    assert.match(barrel, new RegExp(`\\b${exportName}\\b`));
  }

  for (const [relativePath, patterns] of requiredComponents) {
    const content = await readFile(path.join(v211Dir, relativePath), "utf8");

    for (const pattern of patterns) {
      assert.match(
        content,
        new RegExp(`\\b${pattern}\\b`),
        `${relativePath} should use ${pattern}`,
      );
    }
  }
});

test("v2.1.1 keeps TextLike, ModuleId, feature flags, and branding centralized", async () => {
  const [types, branding, app, flags] = await Promise.all([
    readFile(path.join(v211Dir, "types.ts"), "utf8"),
    readFile(path.join(v211Dir, "branding.ts"), "utf8"),
    readFile(path.join(srcDir, "app", "App.vue"), "utf8"),
    readFile(path.join(srcDir, "app", "feature-flags.ts"), "utf8"),
  ]);

  assert.match(types, /import type \{ TextLike \} from '@\/locales'/);
  assert.match(types, /export type \{ TextLike \}/);
  assert.match(types, /export type ModuleId =/);
  assert.match(branding, /productName:\s*'MMMail'/);
  assert.doesNotMatch(branding, /Nexa Workspace|Acme|Workspace/);
  assert.match(app, /@\/design-system\/v211\/theme/);
  assert.match(app, /NConfigProvider/);
  assert.match(flags, /VITE_V211_SHELL/);
  assert.match(flags, /VITE_V211_SHELL\s*!==\s*'false'/);
  assert.match(flags, /VITE_V211_DEBUG_GRID/);
});

test("v2.1.1 shared components are adopted by real routed surfaces", async () => {
  const files = {
    admin: path.join(srcDir, "views", "app", "AdminSectionView.vue"),
    baseLayout: path.join(srcDir, "layouts", "base-layout", "BaseLayout.vue"),
    business: path.join(srcDir, "views", "app", "BusinessOverviewView.vue"),
    contextPanel: path.join(srcDir, "layouts", "modules", "ContextPanel.vue"),
    drive: path.join(srcDir, "views", "app", "DriveSectionView.vue"),
    mail: path.join(srcDir, "views", "app", "MailSurfaceView.vue"),
  };
  const [admin, baseLayout, business, contextPanel, drive, mail] = await Promise.all(
    Object.values(files).map((file) => readFile(file, "utf8")),
  );

  assert.match(baseLayout, /V211AppShell/);
  assert.match(baseLayout, /isV211ShellEnabled/);
  assert.match(business, /V211MetricCard/);
  assert.match(admin, /V211MetricCard/);
  assert.match(mail, /V211DataToolbar/);
  assert.match(drive, /V211DataToolbar/);
  assert.match(contextPanel, /V211RightInsightPanel/);
  assert.match(business, /V211SectionPanel/);
  assert.match(business, /V211EntityList/);
  assert.match(business, /V211StatusTag/);
  assert.match(business, /V211MiniChart/);
  assert.match(drive, /V211ActionBar/);
});

test("v2.1.1 registers ECharts only through the v211 chart wrapper", async () => {
  const packageJson = await readJson(path.join(rootDir, "package.json"));
  const chartIndex = await readFile(path.join(v211Dir, "chart", "index.ts"), "utf8");
  const chartRuntime = await readFile(path.join(v211Dir, "chart", "runtime.ts"), "utf8");
  const sourceFiles = await collectFiles(
    srcDir,
    (file) => (file.endsWith(".ts") && !file.endsWith(".d.ts")) || file.endsWith(".vue"),
  );
  const offenders = [];

  assert.match(packageJson.dependencies?.echarts ?? "", /^\^5\.5\./);
  assert.match(chartIndex, /import\('\.\/runtime'\)/);
  assert.match(chartRuntime, /from 'echarts\/core'/);
  assert.match(chartRuntime, /LineChart/);
  assert.match(chartRuntime, /BarChart/);
  assert.match(chartRuntime, /PieChart/);
  assert.match(chartRuntime, /GaugeChart/);
  assert.match(chartRuntime, /CanvasRenderer/);

  for (const file of sourceFiles) {
    const relativePath = relativeToRepo(file);
    const content = await readFile(file, "utf8");
    const isV211Chart = relativePath.startsWith("frontend-v2/src/design-system/v211/chart/");

    if (!isV211Chart && /echarts|echarts\/core|\.init\(/.test(content)) {
      offenders.push(relativePath);
    }
  }

  assert.deepEqual(offenders, []);
});

test("v2.1.1 has explicit API gap registry entries for every final known gap", async () => {
  const registry = await readJson(path.join(fixturesDir, "v211-api-gap-registry.json"));
  const actualKeys = new Set(registry.map((item) => `${item.module}:${item.field}`));
  const expectedKeys = [
    "sheets:cellComments",
    "sheets:protectedRanges",
    "sheets:chartConfig",
    "pass:rotationSuggestion",
    "pass:sharedMembers",
    "pass:deviceTrust",
    "settings:storageUsageByModule",
    "settings:deviceGeoActivity",
    "settings:integrationHealth",
    "notifications:channelDeliveryTrend",
    "notifications:ruleHitHistory",
    "admin:governanceQueue",
    "admin:tenantCapacityForecast",
    "home:recentFileThumbnail",
    "home:recentEditorAvatar",
  ];

  for (const key of expectedKeys) {
    assert.equal(actualKeys.has(key), true, `missing gap registry key ${key}`);
  }

  for (const item of registry) {
    assert.ok(item.module);
    assert.ok(item.field);
    assert.ok(item.ownerService);
    assert.ok(item.priority);
    assert.match(item.fallback, /empty|NEmpty|derive|reuse|show/i);
  }
});

test("v2.1.1 forbids non-allowlisted native controls in Vue templates", async () => {
  const allowlistPath = path.join(fixturesDir, "v211-native-control-allowlist.json");
  const allowlist = await readJson(allowlistPath);
  const allowed = new Map(allowlist.map((item) => [`${item.file}:${item.tag}`, item]));
  const vueFiles = await collectFiles(srcDir, (file) => file.endsWith(".vue"));
  const offenders = [];

  for (const file of vueFiles) {
    const content = await readFile(file, "utf8");
    const relativePath = relativeToRepo(file);

    for (const tag of findNativeControls(content)) {
      const entry = allowed.get(`${relativePath}:${tag}`);

      if (!entry || !allowedNativeReasons.has(entry.reason) || !entry.removeBy || !entry.owner) {
        offenders.push(`${relativePath}:${tag}`);
      }
    }
  }

  assert.deepEqual(offenders, []);
});
