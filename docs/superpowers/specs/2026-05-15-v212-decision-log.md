# v2.1.2 Decision Log

## Q-8 Browser Verification Tooling

- Date: 2026-05-16
- Decision: add `@playwright/test` and `lighthouse` as Soybean Admin dev-only verification tooling.
- Reason: §26.3 requires Playwright E2E main paths in CI and §27.1 requires Lighthouse performance score above 80. Existing Soybean scripts only covered static contracts, style, i18n, and bundle budget, so these requirements had no executable gate.
- Runtime impact: none. Both packages are `devDependencies` and are not imported by application code.
- Disable/override: `PLAYWRIGHT_CHROMIUM_EXECUTABLE` and `LIGHTHOUSE_CHROME_PATH` can point to a system Chrome; `MMMAIL_LIGHTHOUSE_SKIP_BUILD=1` skips the build step only when the caller has already produced `dist/`.

## Q-9 Coverage Tooling

- Date: 2026-05-16
- Decision: add `vitest`, `@vitest/coverage-v8`, `@vue/test-utils`, and `jsdom` as frontend dev-only coverage tooling for `frontend-v2` and `frontend-admin`.
- Reason: §12.2 and §26.3 require unit coverage, component coverage, and CI-enforced thresholds. Soybean / Naive UI / `@sa/*` provide UI components, layouts, hooks, and request utilities, but they do not provide a test runner, Istanbul/V8 coverage instrumentation, Vue component mounting, or a DOM runtime for component tests. Node's built-in `node:test` can keep static contract tests, but it cannot mount Vue SFCs or enforce per-suite coverage thresholds.
- Runtime impact: none. All four packages are `devDependencies`; application bundles do not import them, and the coverage scripts run only in local validation and CI.
- Size/license/maintenance: dev-only packages are excluded from production bundles; `vitest`, `@vitest/coverage-v8`, `@vue/test-utils`, and `jsdom` use permissive licenses and are actively maintained in the Vue/Vite testing ecosystem.
- Disable/override: coverage gates are explicit scripts (`pnpm test:coverage`, `pnpm test:unit`, `pnpm test:component`) and can be isolated per project; there is no runtime fallback or silent degradation path.
