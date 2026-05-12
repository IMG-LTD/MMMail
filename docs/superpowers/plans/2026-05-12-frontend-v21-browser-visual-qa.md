# Frontend v2.1 Browser Visual QA Implementation Plan

## Goal

Implement a reproducible v2.1 browser visual QA runner for `frontend-v2`, using the local system Chrome and no new browser automation dependency.

## Source

- Master spec: `docs/superpowers/specs/2026-04-28-frontend-v21-ui-upgrade-design.md`, sections 16.4 and 17.
- Slice spec: `docs/superpowers/specs/2026-05-12-frontend-v21-browser-visual-qa-design.md`.

## Target Files

- Add `frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs`.
- Add `frontend-v2/scripts/v21-browser-visual-qa.mjs`.
- Update `frontend-v2/package.json`.
- Generate `docs/superpowers/progress/v21-browser-visual-qa-report.md`.

## Boundaries

- Do not add Playwright, Puppeteer, or lockfile dependency churn.
- Do not commit `.tmp/` screenshots.
- Do not add fake success paths or seeded app data.
- Do not change business UI behavior unless the browser run exposes a real bug.

## Tasks

### Task 1: Add the browser QA contract test

Create a Node test that fails until:

- `package.json` has `visual:qa`.
- The QA script exists.
- The script declares desktop, tablet, and mobile viewports.
- The script covers `/workspace`, `/command-center`, `/notifications`, and `/sheets`.
- The script captures screenshots and writes the browser QA report.

Expected first result: `pnpm --dir frontend-v2 test` fails only in this new test.

### Task 2: Implement the Chrome CDP runner

Create `frontend-v2/scripts/v21-browser-visual-qa.mjs`.

Runner behavior:

- Resolve Chrome from `V21_BROWSER_CHROME`, `google-chrome`, or `google-chrome-stable`.
- Start Vite with a free local port.
- Launch Chrome with a separate temporary profile.
- Use CDP for navigation, viewport sizing, click actions, DOM checks, and screenshots.
- Store screenshots under `.tmp/v21-browser-visual-qa/`.
- Fail with explicit error messages.

### Task 3: Add the package script

Update `frontend-v2/package.json`:

```json
"visual:qa": "node scripts/v21-browser-visual-qa.mjs"
```

### Task 4: Run browser QA and write report

Run:

```bash
pnpm --dir frontend-v2 visual:qa
```

Expected:

- Screenshots are created under `.tmp/v21-browser-visual-qa/`.
- `docs/superpowers/progress/v21-browser-visual-qa-report.md` is generated.
- No `.tmp/` files are staged.

### Task 5: Run full verification

Run:

```bash
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
git diff --cached --check
```

### Task 6: Commit the slice

Stage only:

- `docs/superpowers/specs/2026-05-12-frontend-v21-browser-visual-qa-design.md`
- `docs/superpowers/plans/2026-05-12-frontend-v21-browser-visual-qa.md`
- `docs/superpowers/progress/v21-browser-visual-qa-report.md`
- `frontend-v2/package.json`
- `frontend-v2/scripts/v21-browser-visual-qa.mjs`
- `frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs`

Commit message:

```text
test(frontend-v2): add v2.1 browser visual qa runner
```

## Completion Criteria

- Browser QA command exits 0.
- Contract tests, typecheck, and build pass.
- Report exists and references the screenshot evidence paths.
- `.tmp/` screenshots remain untracked.
