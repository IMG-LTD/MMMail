import { fileURLToPath, URL } from "node:url";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vitest/config";

export const UNIT_COVERAGE_THRESHOLD = 80;
export const COMPONENT_COVERAGE_THRESHOLD = 100;

const suiteName = process.env.MMMAIL_VITEST_SUITE === "component" ? "component" : "unit";

const suites = {
  unit: {
    include: ["tests/unit/**/*.test.ts"],
    coverageInclude: ["src/shared/utils/premium-runtime.ts"],
    threshold: UNIT_COVERAGE_THRESHOLD,
  },
  component: {
    include: ["tests/component/**/*.test.ts"],
    coverageInclude: ["src/design-system/components/StatusBadge.vue"],
    threshold: COMPONENT_COVERAGE_THRESHOLD,
  },
} as const;

const suite = suites[suiteName];

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    coverage: {
      include: [...suite.coverageInclude],
      provider: "v8",
      reportsDirectory: `coverage/${suiteName}`,
      reporter: ["text", "lcov"],
      thresholds: {
        branches: suite.threshold,
        functions: suite.threshold,
        lines: suite.threshold,
        statements: suite.threshold,
      },
    },
    environment: "jsdom",
    include: [...suite.include],
  },
});
