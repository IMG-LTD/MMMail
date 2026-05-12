# Frontend v2.1 Browser Visual QA Design

## 1. Purpose

This specification defines the next v2.1 implementation slice after shared component adoption.

The v2.1 master spec requires browser validation for desktop, tablet, and mobile viewports. Earlier slices added route metadata, shared components, shell wiring, and component-level contracts, but browser validation is still not reproducible from the repository.

This slice adds a local Chrome headless QA runner that captures visual evidence for the v2.1 shell and the shared-component module surfaces.

## 2. Current State

Relevant completed commits:

- `5b67494e feat(frontend-v2): adopt v2.1 shared qa components`
- `27a25d3e feat(frontend-v2): add v2.1 responsive qa components`
- `2d0f08ff docs(frontend-v2): add v2.1 responsive accessibility qa design`

Current gap:

- No committed browser QA script exists.
- No package script can reproduce viewport screenshots.
- No generated report records which routes, overlays, and shared-component surfaces were browser-checked.

## 3. Scope Decision

Use the installed system Chrome directly through the Chrome DevTools Protocol.

Rationale:

- `google-chrome` is available locally.
- `frontend-v2` has no Playwright dependency.
- Adding Playwright just for screenshots would expand dependency surface and lockfile churn.
- CDP is enough for viewport sizing, navigation, button clicks, DOM checks, and screenshots.

## 4. Target Coverage

Viewports:

- Desktop: `1440x900`
- Tablet: `1024x768`
- Mobile: `390x844`

Route scenarios:

- `/workspace` for App Shell, top bar, side nav, mobile nav, and context panel.
- `/command-center` for `ChartCard`, `DataTable`, `TerminalLog`, and `ErrorState`-ready layout.
- `/notifications` for `ChartCard`, `DataTable`, analytics side panel, and responsive layout.
- `/sheets` for `DataTable`, `DataGrid`, and workbook empty/error-ready states.

Overlay scenarios:

- Shell command palette opens through the existing top-bar command button.
- Shell quick-create modal opens through the existing top-bar create button.
- Theme drawer opens through the existing top-bar theme button.

## 5. Non-goals

- Do not commit screenshots.
- Do not add fake backend success data.
- Do not introduce Playwright, Puppeteer, or browser dependencies.
- Do not claim full manual design conformance for every historical design image.
- Do not rewrite large module files in this slice.

## 6. QA Runner Requirements

The runner must:

- Start the local Vite dev server on an available port.
- Launch system Chrome in headless mode with a temporary profile under `.tmp/`.
- Drive Chrome through CDP without external npm dependencies.
- Capture screenshots for each route and viewport under `.tmp/v21-browser-visual-qa/`.
- Click shell controls for command palette, quick-create modal, and theme drawer scenarios.
- Fail explicitly when required selectors are missing, the page is blank, or runtime exceptions occur.
- Write a Markdown report to `docs/superpowers/progress/v21-browser-visual-qa-report.md`.

## 7. Acceptance Criteria

This slice is complete when:

- `frontend-v2/package.json` exposes a `visual:qa` script.
- `frontend-v2/scripts/v21-browser-visual-qa.mjs` runs without Playwright/Puppeteer.
- The browser QA report lists route, viewport, required selectors, and screenshot evidence paths.
- `pnpm --dir frontend-v2 visual:qa` passes locally.
- `pnpm --dir frontend-v2 test`, `typecheck`, and `build` pass after implementation.
