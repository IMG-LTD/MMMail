# Frontend v2.1 Responsive Accessibility Visual QA Design

## 1. Purpose

This specification defines the next v2.1 slice after the API contract catalog: `frontend-v21-responsive-accessibility-visual-qa`.

The slice will focus first on shared UI infrastructure, then use that infrastructure to support responsive, accessibility, and visual QA. This follows the approved direction from the v2.1 master plan section 16.2, where required cross-module components are listed explicitly.

This document is a design specification only. It does not implement frontend code, backend code, migrations, or browser automation scripts.

## 2. Approved Direction

Three implementation directions were considered:

| Option | Summary | Tradeoff |
| --- | --- | --- |
| Shared components first | Build the missing horizontal primitives required by the v2.1 testing strategy. | Strongest foundation; visual QA work becomes repeatable instead of per-page patching. |
| Visual QA first | Start with viewport checks and fix the most visible layout issues. | Faster visible progress, but risks duplicating one-off fixes before component boundaries exist. |
| Design map first | Map every UI image to routes, states, panels, dialogs, and capabilities. | Useful for final conformance, but slower to turn into working product behavior. |

Approved direction: shared components first.

Reason: current `frontend-v2` already has the route catalog, gates, shell, API clients, tokens, and partial design-system primitives. The remaining v2.1 gap is the shared component layer that makes tables, grids, overlays, uploads, commands, charts, logs, and state surfaces consistent across modules.

## 3. Existing Context

Relevant existing foundation:

- `frontend-v2/src/design-system/tokens.ts` and `frontend-v2/src/styles/global.css` provide MMMail tokens, semantic colors, focus styles, reduced-motion support, and high-contrast adjustments.
- Existing primitives include `EmptyState`, `StatusBadge`, `PremiumBadge`, `HostedBadge`, `PremiumGate`, `PermissionGate`, `ProductAccessGate`, and `SectionHeader`.
- `useDialogStack` already tracks modal, drawer, and sheet stack depth.
- `useSrLive` already provides polite and assertive screen-reader live regions.
- `useShellStore` already tracks command palette, notification drawer, quick create, mobile more panel, context panel, and collapsed side nav state.
- `v21-design-system-components.test.mjs`, `accessibility-shell.test.mjs`, and related contract tests already validate part of the design-system and accessibility infrastructure.

Known gap:

- The v2.1 master spec requires `DataTable`, `DataGrid`, `Drawer`, `Modal`, `UploadQueue`, `CommandPalette`, `ErrorState`, `ChartCard`, and `TerminalLog`, but these do not yet exist as shared design-system components.

## 4. UI Design Source Observations

The overview images under `docs/MMMail/UI` share a consistent operational pattern:

- Workspace, Mail, Calendar, Drive, Docs, Sheets/Labs, Pass, Collaboration, Command Center, Notifications, and Settings use a left product nav, top global search, tenant switcher, quick-create action, notifications, help, and user menu.
- Main content is dense and work-focused: KPI tiles, tabs, segmented filters, searchable lists, file tables, activity feeds, permission states, and status chips.
- Right-side surfaces appear repeatedly as drawers, inspectors, composers, comments, share settings, Labs assistant panels, item details, and audit/activity panels.
- Modal and drawer interactions are central: Mail compose, Calendar event editor, Drive share settings, Docs sharing, Sheets protected range, Pass sharing, Admin user/domain/role flows, and destructive confirmations.
- Data-heavy modules need two table families: records tables for Mail/Drive/Notifications/Admin/Command Center, and spreadsheet-style grids for Sheets and permission matrices.
- Upload, async progress, command execution, and audit trails must expose visible state, retry/failure affordances, and `aria-live` announcements.
- Visual language is quiet and utilitarian: white or soft surfaces, thin borders, compact cards, teal primary actions, semantic product accents, rounded controls, and clear status text that is not color-only.

## 5. Component Design

### 5.1 Record DataTable

`DataTable` is the shared table for Mail lists, Drive files, Notifications, Admin users/audit, Command Center tasks, Pass vault items, and Collaboration feeds when they need tabular behavior.

Required API:

- `columns`: readonly column definitions with label, key, width, alignment, sortable, and optional cell slot names.
- `rows`: readonly records.
- `rowKey`: stable string key or callback.
- `selectedKeys`: controlled selection model.
- `loading`, `empty`, `error`, `permissionDenied`, and `premiumLocked` states.
- `density`: comfortable or compact.
- Emits selection, sort, row action, and retry events.

Behavior:

- Keyboard users can move through header controls, row checkboxes, row action buttons, and pagination without pointer use.
- Sort state includes visible label and `aria-sort`.
- Selection state is announced and not conveyed by color alone.
- Empty/error/permission/premium states delegate to shared state components.

### 5.2 Spreadsheet DataGrid

`DataGrid` is the fixed-format grid for Sheets, permission matrices, protected ranges, and review tables.

Required API:

- `columns`, `rows`, `activeCell`, `selectedRange`, and `protectedRanges`.
- `readonly`, `editable`, and `locked` modes.
- `density`, `stickyHeader`, and `stickyFirstColumn`.
- Emits active-cell, range-change, edit-request, and protect-range events.

Behavior:

- Arrow keys move the active cell.
- Header, row number, locked cell, edited cell, and selected range have text or icon cues in addition to color.
- The grid uses stable row heights and column widths so hover states and badges do not resize the layout.

### 5.3 Drawer and Modal

Shared `Drawer` and `Modal` wrappers will standardize Naive UI overlay usage.

Required API:

- Controlled `show` prop.
- `title`, optional `description`, `size`, `tone`, and `closeLabel`.
- Header, body, footer, and actions slots.
- Emits update, close, escape, and after-leave events.

Behavior:

- Integrates with `useDialogStack`.
- Traps focus while open.
- Restores focus to the launcher when closed.
- Supports Escape close unless an explicit dangerous flow disables it with visible copy.
- Uses `aria-modal`, labelled title, and described body text.

### 5.4 UploadQueue

`UploadQueue` supports Drive, Docs, Sheets import, Pass import, and Admin import flows.

Required API:

- `items`: readonly upload entries with id, name, size, progress, status, destination, and error.
- `ariaLabel`, `compact`, and `position`.
- Emits retry, cancel, pause, resume, remove, and open-destination events.

Behavior:

- Progress uses visible text and progress bars.
- Failed uploads expose the original error and retry action.
- State changes are announced through `useSrLive`.
- The component never reports success until the caller marks the item complete.

### 5.5 CommandPalette

`CommandPalette` connects the top search and keyboard-command behavior with existing shell state.

Required API:

- `show`, `query`, `groups`, `recentItems`, and `placeholder`.
- Items support route navigation, command execution, quick create, and hosted/premium/permission state.
- Emits update, close, select, search, and execute events.

Behavior:

- Opens from shell state and keyboard shortcut.
- Uses combobox/listbox semantics.
- Highlights matched text without changing accessible labels.
- Premium, hosted, preview, and permission-restricted commands show badges before selection.

### 5.6 ErrorState

`ErrorState` separates failure surfaces from generic empty states.

Required API:

- `title`, `description`, `errorCode`, `details`, and `retryLabel`.
- Optional `supportActionLabel` and `secondaryActionLabel`.
- Emits retry, support, and secondary actions.

Behavior:

- Does not swallow failures or invent fallback success.
- Shows actionable error context for API, permission, upload, command, and module-load failures.
- Supports inline, card, full-page, and overlay variants.

### 5.7 ChartCard

`ChartCard` wraps metrics and chart-heavy screens while preserving vue-data-ui as the preferred chart layer.

Required API:

- `title`, `description`, `value`, `trend`, `status`, `loading`, and `error`.
- Default chart slot plus fallback text summary.
- Optional action slot and time range slot.

Behavior:

- Shows text summary for screen readers and reduced-motion users.
- Does not rely on color-only legends.
- Stable dimensions prevent chart loading states from shifting dashboards.

### 5.8 TerminalLog

`TerminalLog` supports Command Center runs, background jobs, workflow execution, and diagnostics.

Required API:

- `lines`: readonly log lines with id, timestamp, stream, level, text, and optional command id.
- `running`, `autoFollow`, `filter`, and `copyable`.
- Emits copy, clear, pause-follow, resume-follow, and filter events.

Behavior:

- Uses monospace formatting with high contrast.
- Announces run start, failure, retry, and completion through `useSrLive`.
- Supports keyboard scroll, copy, and filter actions.
- Never fabricates command completion.

## 6. Responsive Design

Responsive behavior targets the master spec viewports:

| Viewport | Target behavior |
| --- | --- |
| 1440x900 desktop | Left nav, top bar, main work surface, and right context/drawer surfaces can coexist. Tables and grids keep dense columns with horizontal overflow only when necessary. |
| 1024x768 tablet | Left nav may collapse. Right context surfaces become drawers. Tables keep primary columns and move secondary actions into row menus. |
| 390x844 mobile | Bottom or compact nav is preferred. Drawers become full-height sheets. Tables become stacked records or horizontally scrollable grids depending on data type. Command palette and upload queue remain reachable without overlapping content. |

The implementation plan must avoid viewport-scaled font sizes. Fixed-format controls need stable dimensions through `minmax`, `aspect-ratio`, fixed row heights, or component-level size tokens.

## 7. Accessibility Design

The component layer must cover these rules:

- Overlay focus trap and focus restoration for Drawer, Modal, CommandPalette, and destructive confirmations.
- Escape behavior is explicit and testable.
- Keyboard navigation for table rows, grid cells, tabs, menus, drawers, and command items.
- `aria-live` announcements for uploads, command runs, background jobs, retries, and failures.
- `prefers-reduced-motion` compatibility for overlays, progress, charts, and command/log updates.
- Status text appears with icon or label, never color alone.
- Contrast must meet operational UI use, including small badges, warning states, and disabled controls.

## 8. Tests and Verification

The implementation plan should add or extend contract tests before code changes:

- Extend `frontend-v2/tests/v21-design-system-components.test.mjs` to require every component from section 16.2.
- Add `frontend-v2/tests/v21-responsive-accessibility-visual-qa-contract.test.mjs` for component APIs, overlay semantics, live-region integration, state variants, and responsive class hooks.
- Extend `frontend-v2/tests/accessibility-shell.test.mjs` for CommandPalette, Drawer, Modal, UploadQueue, and TerminalLog announcements.
- Run `npm run test`, `npm run typecheck`, and `npm run build` from `frontend-v2` for the implementation slice.

Browser validation remains a later sub-step after the shared components exist. It must cover 1440x900, 1024x768, and 390x844 against App Shell, top bar, side/mobile nav, context panel, tables, drawers, modals, locked states, denied states, and empty/error/loading states.

## 9. Acceptance Criteria

This slice is complete when:

- All required section 16.2 components exist as shared frontend-v2 components.
- Existing `EmptyState`, `StatusBadge`, `PremiumGate`, and `PermissionGate` remain compatible with their current contract tests.
- Table, grid, overlay, upload, command, chart, error, and log behavior is reusable across modules without module-specific duplicate primitives.
- Component APIs are typed and documented through tests.
- Overlay, keyboard, live-region, reduced-motion, and non-color-only status expectations are covered by automated tests.
- Frontend test, typecheck, and build commands pass for the slice.

## 10. Explicit Non-Goals

- Do not redesign the full route IA in this slice.
- Do not implement backend endpoints.
- Do not add mock success paths for uploads, commands, or API failures.
- Do not silently hide unsupported premium, hosted, or permission-restricted capabilities.
- Do not perform full visual QA before the shared component foundation exists.
