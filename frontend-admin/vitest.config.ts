import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';

export const UNIT_COVERAGE_THRESHOLD = 80;
export const COMPONENT_COVERAGE_THRESHOLD = 80;

const suiteName = process.env.MMMAIL_VITEST_SUITE === 'component' ? 'component' : 'unit';

const suites = {
  unit: {
    include: ['tests/unit/**/*.test.ts'],
    coverageInclude: [
      'src/router/routes/access-meta.ts',
      'src/utils/common.ts',
      'src/utils/storage.ts',
      'src/locales/langs/v212-error-messages.ts'
    ],
    threshold: UNIT_COVERAGE_THRESHOLD
  },
  component: {
    include: ['tests/component/**/*.test.ts'],
    coverageInclude: [
      'src/components/custom/button-icon.vue',
      'src/components/feedback/EmptyState.vue',
      'src/components/feedback/ErrorState.vue',
      'src/components/feedback/LoadingState.vue',
      'src/components/feedback/PageStateWrapper.vue',
      'src/components/access/EntitlementGate.vue',
      'src/views/business-overview/index.vue',
      'src/views/business-overview/modules/overview-stats.vue'
    ],
    threshold: COMPONENT_COVERAGE_THRESHOLD
  }
} as const;

const suite = suites[suiteName];

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '~': fileURLToPath(new URL('./', import.meta.url)),
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    coverage: {
      include: [...suite.coverageInclude],
      provider: 'v8',
      reportsDirectory: `coverage/${suiteName}`,
      reporter: ['text', 'lcov', 'json-summary'],
      thresholds: {
        branches: suite.threshold,
        functions: suite.threshold,
        lines: suite.threshold,
        statements: suite.threshold
      }
    },
    environment: 'jsdom',
    include: [...suite.include]
  }
});
