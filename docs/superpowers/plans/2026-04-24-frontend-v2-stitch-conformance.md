# Frontend v2 Stitch Conformance Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce an evidence-backed audit report that states whether the current `frontend-v2` implementation fully conforms to the existing MMMail Stitch designs.

**Architecture:** This is a review-and-report workflow, not an implementation change. The work inventories Stitch screens and design systems, inventories current Vue routes and shell navigation, captures local browser evidence through gstack `/browse`, maps routes to Stitch screens, compares each page across the approved dimensions, and writes one report with severity-ranked findings.

**Tech Stack:** Stitch MCP tools, Vue 3, TypeScript, Vite, Vue Router, Pinia, Naive UI, pnpm, gstack `/browse`, Markdown report output.

---

## File Structure

- Create: `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`
  - Final audit report required by the approved spec.
  - Contains the Stitch inventory, frontend route inventory, mapping table, conformance matrix, severity summary, blocked items, repair order, and recommendation.
- Use local evidence directory: `.tmp/frontend-v2-stitch-conformance-2026-04-24/`
  - Browser screenshots and temporary capture notes.
  - Do not commit this directory.
- Read: `docs/superpowers/specs/2026-04-24-frontend-v2-stitch-conformance-design.md`
  - Approved audit design and acceptance criteria.
- Read: `frontend-v2/src/app/router/routes.ts`
  - Source of truth for reachable frontend routes.
- Read: `frontend-v2/src/layouts/modules/shell-nav.ts`
  - Source of truth for shell navigation and mobile tabs.
- Read: `frontend-v2/src/layouts/**`
  - Shell, top bar, side navigation, and mobile navigation implementation.
- Read: `frontend-v2/src/views/**`
  - Vue page entry files for route-to-component mapping and source evidence.
- Read when relevant: `frontend-v2/src/shared/**`, `frontend-v2/src/store/**`, `frontend-v2/src/app/App.vue`
  - Shared content, stores, onboarding behavior, providers, and shell-level page states.

## Fixed Audit Inputs

### Primary Stitch project

Use this as the design source of truth unless the user explicitly changes the project:

| Field | Value |
| --- | --- |
| Project name | `projects/3947413193814542306` |
| Project title | `MMMail UI UX Handoff v1.8` |
| Project type | `PROJECT_DESIGN` |
| Visibility | `PRIVATE` |
| Device type | `DESKTOP` |
| Updated | `2026-04-19T11:59:51.618545Z` |

Do not use `docs/assets`, generated mockups, or historical UAT screenshots as conformance sources. They may be mentioned only as background context.

### Primary design-system source

Treat `assets/17183685925114722083` (`MMMail v1.8 Design System`, version `2`) as the primary project design system because it matches the project theme. Record the additional attached design systems as supporting systems when a screen title or visual convention clearly references them:

| Design system | Name | Use in report |
| --- | --- | --- |
| `assets/17183685925114722083` | `MMMail v1.8 Design System` | Primary MMMail UI v1.8 tokens and visual rules |
| `assets/0f9455d7caa84f02bbeaefb81b1a4d87` | `Helvetia Trust` | Secure delivery/public access support |
| `assets/42b3178c6c1349d7b6a27f6d7323c40c` | `Alpine Grid` | Pass-specific support |
| `assets/5670db0a1ae24e049854b220d8f300a9` | `HelvetiPass Grid` | Pass-specific support |
| `assets/9369320cf8ee40819f3a1c9d62f034f7` | `MMMail Swiss Mono` | Alternate Swiss system support |
| `assets/943f610fe29547438689b01011909db7` | `MMMail Swiss Mint` | Calendar-specific support |
| `assets/b9e98147c7b04649b0850dd46aed085e` | `MMMail International / V1.8` | Governance/admin support |

Core project theme constraints to cite in the report when relevant:

- Swiss / International productivity system.
- Calm neutral surfaces.
- Precise 4px spacing.
- Restrained accent color.
- Privacy-first mood.
- Strong border definition.
- Clear three-pane shell.
- Compact-but-readable information density.
- Progressive disclosure for encryption details.
- Avoid decorative gradients.
- Avoid generic purple SaaS styling.
- Keep security terminology out of primary surfaces.
- Support light and dark mode.
- Desktop-first layouts and mobile single-stack degradation.
- Fonts: Inter and Public Sans.
- Key colors: `background #f8f9fa`, `surface #f8f9fa`, `primary #566070`, `tertiary #005bc4`.

### Primary Stitch screen inventory

These 27 screens must appear in the report inventory:

| Screen title | Screen resource |
| --- | --- |
| `Login` | `projects/3947413193814542306/screens/fe84eb6ad18d4f5b99dfeb87ef98c2fa` |
| `Register` | `projects/3947413193814542306/screens/03ab65e262004119aaeebcc67ad9ec13` |
| `Product Access Blocked` | `projects/3947413193814542306/screens/f07beab713fe4865a3beab1cb1b8908c` |
| `Secure Share Access` | `projects/3947413193814542306/screens/246252445c70478586d55c3345782911` |
| `Invitation Landing` | `projects/3947413193814542306/screens/63735e757394422cb62b7ff6a31ccfc5` |
| `MMMail - Inbox` | `projects/3947413193814542306/screens/2d84f75c0d014f61882498e871512b4c` |
| `Inbox with Recovery Card` | `projects/3947413193814542306/screens/ac3b9413552f428a96c5fb8c09907f41` |
| `Inbox with Onboarding Progress` | `projects/3947413193814542306/screens/ad6f057cb18f4b9cbd518d007da4aa2b` |
| `Compose - Minimized State` | `projects/3947413193814542306/screens/41577976dfa34e01b1dde2b38b40b543` |
| `Compose - Normal State` | `projects/3947413193814542306/screens/24731233718740c3a8564f2e3a2ec744` |
| `Compose - Expanded State` | `projects/3947413193814542306/screens/d998eb83af0c44acbb225e077fb27850` |
| `First Compose with Green Lock` | `projects/3947413193814542306/screens/cd9a56c1c6674deb97866fcdb6d3432e` |
| `MMMail Calendar - Week View with Edit Drawer` | `projects/3947413193814542306/screens/6bc2623562884a7a9f87de71ddece5f4` |
| `MMMail Calendar - Week View with Edit Drawer` | `projects/3947413193814542306/screens/fefc16936e7b4d6b8bd1dd70d39a19cd` |
| `MMMail Drive - File Explorer` | `projects/3947413193814542306/screens/7813d3b0aaff44579b6ac2a238d7531e` |
| `Pass Workspace` | `projects/3947413193814542306/screens/812b741ab3414b70b6f1c46ebebcd108` |
| `Pass Monitor` | `projects/3947413193814542306/screens/4964ec165e554525a43670a74add1703` |
| `Settings Workspace` | `projects/3947413193814542306/screens/a86679a6f62e4f44a55095f0f2f80dde` |
| `Business Overview` | `projects/3947413193814542306/screens/6017a091529a4be5be04d1ad1e8b79be` |
| `Suite Overview - Mainline Journey` | `projects/3947413193814542306/screens/04750c2c9c8f4b6cb9f7cd625a63280e` |
| `Suite Overview - Today's Next Steps` | `projects/3947413193814542306/screens/3e33fd3b8b0241e0a802d820d1e85872` |
| `Security Center` | `projects/3947413193814542306/screens/7e43de59143e4fbeba37e1016eca47d0` |
| `Organizations Product Access Matrix` | `projects/3947413193814542306/screens/a3e9b00608ec4a5a8b4517369ad8b66b` |
| `Retention Banners Overview` | `projects/3947413193814542306/screens/60955681195a49a684f1f7200d0c764f` |
| `Create Password and Prepare Privacy` | `projects/3947413193814542306/screens/51b2ce951d0d4351b4838de379a83a70` |
| `Cross-Device Verification Dialog` | `projects/3947413193814542306/screens/11af52a0caf6474cad478a617d8c5542` |
| `Recovery Kit Download Drawer` | `projects/3947413193814542306/screens/218e0f4152ed419ab241d6052339a228` |

### Secondary Stitch projects

Record these in a short note, but do not use them to mark frontend pages as conforming unless the user later changes the source-of-truth project:

| Project | Title | Report handling |
| --- | --- | --- |
| `projects/14058266156772737751` | `MMMail Mail List Detail Prototype` | Secondary mail prototype only |
| `projects/17704273601208292715` | `MMMail UI Redesign` | Different redesign direction only |

## Report Status Vocabulary

Use these exact status values in the conformance matrix:

- `Conforms`
- `Partially conforms`
- `Does not conform`
- `Stitch design source missing`
- `Blocked`
- `Not reachable`

A row can use `Conforms` only when both conditions are true:

1. A matching Stitch screen exists in the primary project.
2. The implementation route and Vue entry file were inspected.

## Severity Vocabulary

Use these exact severities for non-conformance findings:

- `P0`: page or critical flow is unreachable, or implementation is obviously not the same design.
- `P1`: main layout, information architecture, or key component state significantly diverges.
- `P2`: visual detail divergence such as spacing, color, density, icon treatment, or text hierarchy.
- `P3`: minor polish issue or acceptable difference that should not block current use.

---

### Task 1: Create the Stitch source inventory

**Files:**
- Create or modify: `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`
- Evidence source: Stitch project `projects/3947413193814542306`

- [ ] **Step 1: Fetch the primary Stitch project metadata**

Use this Stitch tool input:

```json
{
  "tool": "mcp__stitch__get_project",
  "input": {
    "name": "projects/3947413193814542306"
  }
}
```

Expected: the returned project title is `MMMail UI UX Handoff v1.8`.

- [ ] **Step 2: Fetch the primary Stitch screen list**

Use this Stitch tool input:

```json
{
  "tool": "mcp__stitch__list_screens",
  "input": {
    "projectId": "3947413193814542306"
  }
}
```

Expected: the returned list contains the 27 screen resources listed in this plan.

- [ ] **Step 3: Fetch the attached design systems**

Use this Stitch tool input:

```json
{
  "tool": "mcp__stitch__list_design_systems",
  "input": {
    "projectId": "3947413193814542306"
  }
}
```

Expected: the returned list contains `assets/17183685925114722083` with display name `MMMail v1.8 Design System`.

- [ ] **Step 4: Create the initial report with this exact top-level structure**

Write `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md` with these headings:

```markdown
# Frontend v2 Stitch Conformance Review — 2026-04-24

## Executive summary

## 1. Stitch project and design-source inventory

## 2. frontend-v2 route/page inventory

## 3. Stitch screen to frontend route mapping

## 4. Per-page conformance matrix

## 5. P0/P1/P2/P3 issue summary

## 6. Items that could not be verified

## 7. Recommended repair order

## 8. Implementation-fix phase recommendation

## Appendix A. Evidence index
```

- [ ] **Step 5: Fill section 1 with the project, design-system, and screen inventory**

Use the fixed audit inputs from this plan and the fresh Stitch tool results. The screen table must include these columns:

```markdown
| Screen title | Screen resource | Device | Width | Height | Design-system notes |
| --- | --- | --- | ---: | ---: | --- |
```

For duplicate `MMMail Calendar - Week View with Edit Drawer` rows, keep both screen resources and add a note that the title is duplicated in Stitch.

- [ ] **Step 6: Verify Task 1 output**

Run:

```bash
grep -c "projects/3947413193814542306/screens/" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "assets/17183685925114722083" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
```

Expected: first command prints at least `27`; second command prints one or more lines.

- [ ] **Step 7: Commit Task 1 audit report progress**

```bash
git add "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
git commit -m "docs: start frontend stitch conformance report"
```

---

### Task 2: Build the frontend route and page inventory

**Files:**
- Modify: `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`
- Read: `frontend-v2/src/app/router/routes.ts`
- Read: `frontend-v2/src/layouts/modules/shell-nav.ts`
- Read: `frontend-v2/src/app/App.vue`
- Read: `frontend-v2/src/layouts/**`
- Read: `frontend-v2/src/views/**`

- [ ] **Step 1: Inspect route definitions with line numbers**

Use the Read tool on:

```text
/home/xiang/桌面/project/MMMail-test/MMMail/frontend-v2/src/app/router/routes.ts
```

Record actual line references for each route group in section 2.

- [ ] **Step 2: Inspect shell navigation with line numbers**

Use the Read tool on:

```text
/home/xiang/桌面/project/MMMail-test/MMMail/frontend-v2/src/layouts/modules/shell-nav.ts
```

Record line references for desktop navigation groups and mobile tabs.

- [ ] **Step 3: Inspect app-level layout selection**

Use the Read tool on:

```text
/home/xiang/桌面/project/MMMail-test/MMMail/frontend-v2/src/app/App.vue
```

Record the line reference showing how `BaseLayout` and `BlankLayout` are selected.

- [ ] **Step 4: Inventory public routes**

Section 2 must include every route in this public-route set, with the actual Vue entry file from `routes.ts`:

```text
/
/login
/register
/boundary
/product-access-blocked
/share/mail/:token
/share/drive/:token
/public/drive/shares/:token
/share/pass/:token
/onboarding/:storyKey
/failure-modes
/failure-modes/:storyKey
/404
/500
/offline
/maintenance
```

- [ ] **Step 5: Inventory mail routes**

Section 2 must include every route in this mail-route set, with the actual Vue entry file from `routes.ts`:

```text
/inbox
/starred
/snoozed
/drafts
/scheduled
/outbox
/sent
/archive
/spam
/trash
/unread
/contacts
/search
/compose
/conversations/:id
/folders/:id
/labels/:id
/mail/:id
```

Mark `/mail/:id` as a redirect route if the current route table still defines it that way.

- [ ] **Step 6: Inventory workspace and product routes**

Section 2 must include every route in this workspace/product-route set, with the actual Vue entry file from `routes.ts`:

```text
/calendar
/drive
/drive/shared
/drive/recent
/drive/starred
/drive/trash
/pass
/pass/shared-library
/pass/secure-links
/pass/alias-center
/pass/mailbox
/pass/business-policy
/pass/monitor
/docs
/docs/:id
/sheets
/sheets/:id
```

- [ ] **Step 7: Inventory aggregation, governance, and research routes**

Section 2 must include every route in this route set, with the actual Vue entry file from `routes.ts`:

```text
/collaboration
/command-center
/notifications
/suite
/suite/plans
/suite/billing
/suite/operations
/suite/boundary
/business
/organizations
/organizations/members
/organizations/product-access
/organizations/domains
/organizations/mail-identities
/organizations/policy
/organizations/monitor
/organizations/session-monitor
/organizations/audit
/security
/settings
/labs
/labs/:moduleKey
```

- [ ] **Step 8: Inventory shell navigation coverage**

Section 2 must include a shell navigation table with these columns:

```markdown
| Shell area | Navigation item | Route | Source reference |
| --- | --- | --- | --- |
```

Include desktop shell groups `Workspace`, `Aggregation`, `Governance`, and `Research`. Include mobile tabs `Mail`, `Calendar`, `Drive`, `Pass`, and `More`.

- [ ] **Step 9: Verify Task 2 output**

Run:

```bash
grep -n "| /inbox |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "| /labs/:moduleKey |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "Mobile" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
```

Expected: each command prints at least one matching line.

- [ ] **Step 10: Commit Task 2 audit report progress**

```bash
git add "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
git commit -m "docs: inventory frontend v2 routes for stitch audit"
```

---

### Task 3: Build the Stitch-to-route mapping table

**Files:**
- Modify: `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`
- Read: `frontend-v2/src/app/router/routes.ts`
- Read: `frontend-v2/src/views/**`
- Evidence source: Stitch screens from `projects/3947413193814542306`

- [ ] **Step 1: Add the mapping table structure**

In section 3, add this exact table structure:

```markdown
| Stitch screen / variant | Stitch resource | Frontend route | Vue entry file and key child components | Required auth/session/role state | Capture status | Mapping confidence | Mapping evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |
```

- [ ] **Step 2: Add high-confidence public mappings**

Add these rows and update Vue entry file names from the current route table:

```markdown
| `Login` | `projects/3947413193814542306/screens/fe84eb6ad18d4f5b99dfeb87ef98c2fa` | `/login` | route entry from `frontend-v2/src/app/router/routes.ts`; login view entry file | Anonymous session | Pending browser capture | high | Screen title and route name both target login. |
| `Register` | `projects/3947413193814542306/screens/03ab65e262004119aaeebcc67ad9ec13` | `/register` | route entry from `frontend-v2/src/app/router/routes.ts`; register view entry file | Anonymous session | Pending browser capture | high | Screen title and route name both target registration. |
| `Product Access Blocked` | `projects/3947413193814542306/screens/f07beab713fe4865a3beab1cb1b8908c` | `/product-access-blocked` | route entry from `frontend-v2/src/app/router/routes.ts`; blocked view entry file | Product access denied state | Pending browser capture | high | Screen title matches route purpose. |
| `Secure Share Access` | `projects/3947413193814542306/screens/246252445c70478586d55c3345782911` | `/share/mail/:token`, `/share/drive/:token`, `/public/drive/shares/:token`, `/share/pass/:token` | route entries from `frontend-v2/src/app/router/routes.ts`; public share view files | Public token state | Pending browser capture | medium | Stitch screen is generic secure-share access while implementation has multiple product share routes. |
```

- [ ] **Step 3: Add high-confidence core app mappings**

Add these rows and update Vue entry file names from the current route table:

```markdown
| `MMMail - Inbox` | `projects/3947413193814542306/screens/2d84f75c0d014f61882498e871512b4c` | `/inbox` | inbox route entry and mail workspace child components | Authenticated user | Pending browser capture | high | Screen title matches inbox. |
| `Inbox with Recovery Card` | `projects/3947413193814542306/screens/ac3b9413552f428a96c5fb8c09907f41` | `/inbox` | inbox route entry and recovery/onboarding child components if present | Authenticated user with recovery prompt state | Pending browser capture | medium | Same inbox shell with a specific card state. |
| `Inbox with Onboarding Progress` | `projects/3947413193814542306/screens/ad6f057cb18f4b9cbd518d007da4aa2b` | `/inbox` | inbox route entry and onboarding progress child components if present | Authenticated first-login or onboarding state | Pending browser capture | medium | Same inbox shell with onboarding progress state. |
| `Compose - Minimized State` | `projects/3947413193814542306/screens/41577976dfa34e01b1dde2b38b40b543` | `/compose` | compose route entry and compose component state | Authenticated user with minimized composer | Pending browser capture | medium | Screen title maps to compose component state. |
| `Compose - Normal State` | `projects/3947413193814542306/screens/24731233718740c3a8564f2e3a2ec744` | `/compose` | compose route entry and compose component state | Authenticated user | Pending browser capture | high | Screen title maps to default compose route. |
| `Compose - Expanded State` | `projects/3947413193814542306/screens/d998eb83af0c44acbb225e077fb27850` | `/compose` | compose route entry and compose component state | Authenticated user with expanded composer | Pending browser capture | medium | Screen title maps to compose component state. |
| `First Compose with Green Lock` | `projects/3947413193814542306/screens/cd9a56c1c6674deb97866fcdb6d3432e` | `/compose` | compose route entry and secure-send child components if present | Authenticated first-compose state | Pending browser capture | medium | Same compose flow with first-use secure state. |
| `MMMail Calendar - Week View with Edit Drawer` | `projects/3947413193814542306/screens/6bc2623562884a7a9f87de71ddece5f4` | `/calendar` | calendar route entry and drawer child components | Authenticated user with event edit drawer | Pending browser capture | high | Screen title maps to calendar route and edit drawer state. |
| `MMMail Calendar - Week View with Edit Drawer` | `projects/3947413193814542306/screens/fefc16936e7b4d6b8bd1dd70d39a19cd` | `/calendar` | calendar route entry and drawer child components | Authenticated user with event edit drawer | Pending browser capture | high | Duplicate title in Stitch; keep both resources in evidence. |
| `MMMail Drive - File Explorer` | `projects/3947413193814542306/screens/7813d3b0aaff44579b6ac2a238d7531e` | `/drive` | drive route entry and file explorer child components | Authenticated user | Pending browser capture | high | Screen title maps to drive file explorer. |
```

- [ ] **Step 4: Add product and governance mappings**

Add these rows and update Vue entry file names from the current route table:

```markdown
| `Pass Workspace` | `projects/3947413193814542306/screens/812b741ab3414b70b6f1c46ebebcd108` | `/pass` | pass route entry and pass workspace child components | Authenticated user | Pending browser capture | high | Screen title maps to Pass workspace. |
| `Pass Monitor` | `projects/3947413193814542306/screens/4964ec165e554525a43670a74add1703` | `/pass/monitor` | pass monitor route entry and monitor child components | Authenticated user | Pending browser capture | high | Screen title maps to Pass monitor route. |
| `Settings Workspace` | `projects/3947413193814542306/screens/a86679a6f62e4f44a55095f0f2f80dde` | `/settings` | settings route entry and settings panels | Authenticated user | Pending browser capture | high | Screen title maps to settings route. |
| `Business Overview` | `projects/3947413193814542306/screens/6017a091529a4be5be04d1ad1e8b79be` | `/business` | business route entry and business overview components | Authenticated user with business access | Pending browser capture | high | Screen title maps to business route. |
| `Suite Overview - Mainline Journey` | `projects/3947413193814542306/screens/04750c2c9c8f4b6cb9f7cd625a63280e` | `/suite` | suite route entry and suite overview components | Authenticated user | Pending browser capture | high | Screen title maps to suite overview. |
| `Suite Overview - Today's Next Steps` | `projects/3947413193814542306/screens/3e33fd3b8b0241e0a802d820d1e85872` | `/suite` | suite route entry and next-step components if present | Authenticated user with next-step state | Pending browser capture | medium | Same suite route with a specific state. |
| `Security Center` | `projects/3947413193814542306/screens/7e43de59143e4fbeba37e1016eca47d0` | `/security` | security route entry and security center components | Authenticated user | Pending browser capture | high | Screen title maps to security route. |
| `Organizations Product Access Matrix` | `projects/3947413193814542306/screens/a3e9b00608ec4a5a8b4517369ad8b66b` | `/organizations/product-access` | organizations product-access route entry and matrix components | Authenticated organization admin | Pending browser capture | high | Screen title maps to organization product-access route. |
```

- [ ] **Step 5: Add low-confidence and state-only mappings**

Add these rows and update Vue entry file names from the current route table:

```markdown
| `Invitation Landing` | `projects/3947413193814542306/screens/63735e757394422cb62b7ff6a31ccfc5` | `/onboarding/:storyKey` or public invitation-related route if present | route entry from `frontend-v2/src/app/router/routes.ts` | Anonymous or invited user | Pending browser capture | low | Stitch title suggests invitation landing; current route naming must confirm the nearest implementation. |
| `Retention Banners Overview` | `projects/3947413193814542306/screens/60955681195a49a684f1f7200d0c764f` | route containing retention banner components after source inspection | route entry and banner component files after source inspection | Authenticated user with banner state | Pending browser capture | low | Screen is a component-state overview rather than an obvious route. |
| `Create Password and Prepare Privacy` | `projects/3947413193814542306/screens/51b2ce951d0d4351b4838de379a83a70` | `/onboarding/:storyKey` or registration/password setup route if present | route entry and password setup components after source inspection | New user setup state | Pending browser capture | low | Screen is an onboarding/setup state; route must be confirmed from source. |
| `Cross-Device Verification Dialog` | `projects/3947413193814542306/screens/11af52a0caf6474cad478a617d8c5542` | route containing verification dialog after source inspection | route entry and verification dialog component files after source inspection | Authenticated user with cross-device verification state | Pending browser capture | low | Screen is a modal/dialog state rather than an obvious route. |
| `Recovery Kit Download Drawer` | `projects/3947413193814542306/screens/218e0f4152ed419ab241d6052339a228` | route containing recovery-kit drawer after source inspection | route entry and drawer component files after source inspection | Authenticated user with recovery-kit state | Pending browser capture | low | Screen is a drawer state rather than an obvious route. |
```

If source inspection finds no matching implementation for any low-confidence row, set the route field to `No matching frontend route found` and the conformance status to `Stitch design source exists, implementation missing` in the issue summary.

- [ ] **Step 6: Add missing-design rows for frontend routes without primary Stitch screens**

Every route from Task 2 that lacks a matching primary Stitch screen must appear in section 3 with:

```text
Stitch screen / variant: Stitch design source missing
Stitch resource: none in primary project
Mapping confidence: missing
Capture status: captured if reachable, blocked if route cannot be opened
```

At minimum, assess whether these route groups lack primary Stitch screens: boundary pages, failure-mode pages, offline/maintenance/error pages, Docs, Sheets, Labs, Collaboration, Command Center, Notifications, Drive sub-routes, Pass sub-routes beyond `/pass` and `/pass/monitor`, Organizations sub-routes beyond `/organizations/product-access`, Suite sub-routes beyond `/suite`, and mail folder/detail routes beyond `/inbox` and `/compose`.

- [ ] **Step 7: Verify Task 3 output**

Run:

```bash
grep -n "Mapping confidence" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "Stitch design source missing" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "Organizations Product Access Matrix" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
```

Expected: each command prints at least one matching line.

- [ ] **Step 8: Commit Task 3 audit report progress**

```bash
git add "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
git commit -m "docs: map stitch screens to frontend v2 routes"
```

---

### Task 4: Prepare local frontend evidence capture

**Files:**
- Modify: `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`
- Create local files only: `.tmp/frontend-v2-stitch-conformance-2026-04-24/**`
- Read: `frontend-v2/package.json`
- Read: `frontend-v2/src/store/modules/**`
- Read: `frontend-v2/src/shared/**`

- [ ] **Step 1: Inspect frontend scripts**

Use the Read tool on:

```text
/home/xiang/桌面/project/MMMail-test/MMMail/frontend-v2/package.json
```

Confirm the dev script still starts Vite on `127.0.0.1:5174`.

- [ ] **Step 2: Verify dependencies or install them**

Run:

```bash
pnpm --dir "frontend-v2" install
```

Expected: pnpm completes successfully. If the environment cannot install dependencies, record `Blocked` in section 6 with the exact command and error summary.

- [ ] **Step 3: Run frontend sanity checks**

Run:

```bash
pnpm --dir "frontend-v2" typecheck
pnpm --dir "frontend-v2" test
```

Expected: both commands pass. If either command fails, record the failing command and continue visual audit only for pages that can still be run locally.

- [ ] **Step 4: Start the local frontend dev server**

Run the dev server as a long-running process:

```bash
pnpm --dir "frontend-v2" dev
```

Expected: Vite serves the frontend at `http://127.0.0.1:5174`.

- [ ] **Step 5: Create the local evidence directory**

Run:

```bash
ls ".tmp"
mkdir -p ".tmp/frontend-v2-stitch-conformance-2026-04-24/desktop" ".tmp/frontend-v2-stitch-conformance-2026-04-24/narrow" ".tmp/frontend-v2-stitch-conformance-2026-04-24/mobile"
```

Expected: directories exist under `.tmp/frontend-v2-stitch-conformance-2026-04-24/`.

- [ ] **Step 6: Use gstack `/browse` for browser work**

All browser opening and screenshot capture must use gstack `/browse`. Do not use direct Chrome MCP tools.

Use these viewport targets:

```text
Desktop: 1440x900
Narrow desktop/tablet: 1024x768
Mobile: 390x844
```

Capture desktop screenshots for every reachable route. Capture narrow and mobile screenshots for shell-sensitive pages and any screen where Stitch or the shell behavior indicates responsive intent.

- [ ] **Step 7: Use the exact screenshot naming convention**

Use this naming convention:

```text
.tmp/frontend-v2-stitch-conformance-2026-04-24/<viewport>/<route-slug>.png
```

Route slug rule:

```text
/ -> root
/login -> login
/share/mail/:token -> share__mail__token
/organizations/product-access -> organizations__product-access
/labs/:moduleKey -> labs__moduleKey
```

- [ ] **Step 8: Record capture limitations**

In section 6, record any route that cannot be captured because credentials, fixture data, public share tokens, organization roles, or local services are unavailable. Each blocked row must include:

```markdown
| Route/state | Blocker | Evidence attempted | Result |
| --- | --- | --- | --- |
```

- [ ] **Step 9: Verify Task 4 output**

Run:

```bash
find ".tmp/frontend-v2-stitch-conformance-2026-04-24" -maxdepth 2 -type f -name "*.png" | sort | wc -l
grep -n "Items that could not be verified" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
```

Expected: the first command prints the number of screenshots captured; the second command prints the section heading.

- [ ] **Step 10: Commit Task 4 report updates only**

Do not commit `.tmp` screenshots.

```bash
git add "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
git commit -m "docs: record frontend v2 capture evidence plan"
```

---

### Task 5: Compare public routes and setup/onboarding states

**Files:**
- Modify: `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`
- Evidence: Stitch screens `Login`, `Register`, `Product Access Blocked`, `Secure Share Access`, `Invitation Landing`, setup/recovery screens
- Evidence: screenshots in `.tmp/frontend-v2-stitch-conformance-2026-04-24/**`
- Read: public route view files from `frontend-v2/src/views/**`

- [ ] **Step 1: Add conformance rows for public mapped screens**

In section 4, add rows for:

```text
/login
/register
/product-access-blocked
/share/mail/:token
/share/drive/:token
/public/drive/shares/:token
/share/pass/:token
/onboarding/:storyKey
```

- [ ] **Step 2: Compare each public route across the approved dimensions**

For each row, fill these columns:

```markdown
| Page/state | Stitch source | Frontend source | Browser evidence | Layout structure | Visual system | Component states | Content/i18n | Interaction entry points | Responsive behavior | Design-system usage | Result | Findings |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
```

Use concise evidence text, for example:

```markdown
| `/login` | `Login` (`.../screens/fe84...`) | `frontend-v2/src/app/router/routes.ts:<line>` and login view file | `.tmp/.../desktop/login.png` | Matches centered auth surface or list the difference | Matches neutral Swiss palette or list the difference | Login form state inspected | Labels inspected | Submit/register links inspected | Desktop captured; mobile captured if shell-free responsive state exists | Compared to `MMMail v1.8 Design System` | `Partially conforms` | `P2: spacing density differs from Stitch by visual inspection.` |
```

- [ ] **Step 3: Treat missing design sources explicitly**

For public routes without a matching primary Stitch screen, set:

```text
Result: Stitch design source missing
Findings: No primary Stitch screen in `projects/3947413193814542306`; not eligible for `Conforms`.
```

At minimum, assess `/boundary`, `/failure-modes`, `/failure-modes/:storyKey`, `/404`, `/500`, `/offline`, and `/maintenance` this way unless a matching primary Stitch screen is found during source inspection.

- [ ] **Step 4: Add public-route findings to section 5**

Every `Does not conform` or `Partially conforms` row must have a severity item with:

```markdown
| Severity | Page/state | Finding | Evidence | Suggested fix direction |
| --- | --- | --- | --- | --- |
```

- [ ] **Step 5: Verify Task 5 output**

Run:

```bash
grep -n "| /login |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "| /product-access-blocked |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "Stitch design source missing" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
```

Expected: each command prints at least one matching line.

- [ ] **Step 6: Commit Task 5 audit report progress**

```bash
git add "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
git commit -m "docs: compare public frontend routes with stitch"
```

---

### Task 6: Compare the shell, Mail, Calendar, Drive, and Settings routes

**Files:**
- Modify: `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`
- Evidence: Stitch screens for Inbox, Compose, Calendar, Drive, Settings, onboarding/recovery states
- Evidence: screenshots in `.tmp/frontend-v2-stitch-conformance-2026-04-24/**`
- Read: `frontend-v2/src/layouts/**`
- Read: `frontend-v2/src/views/**`
- Read: `frontend-v2/src/shared/components/WelcomeOnboardingModal.vue`
- Read: `frontend-v2/src/store/modules/onboarding.ts`

- [ ] **Step 1: Add shell conformance rows**

In section 4, add rows for:

```text
Base shell desktop
Base shell mobile tabs
Top bar
Side navigation
Mobile navigation
Theme/locale/system shell behavior if visible in source or browser
```

Use `frontend-v2/src/layouts/modules/shell-nav.ts` as source evidence and cite the relevant line numbers.

- [ ] **Step 2: Add Mail conformance rows**

Add rows for:

```text
/inbox
/compose
/conversations/:id
/mail folder routes: /starred, /snoozed, /drafts, /scheduled, /outbox, /sent, /archive, /spam, /trash, /unread, /contacts, /search, /folders/:id, /labels/:id
```

For folder routes that reuse the inbox/list workspace, state the reuse explicitly and cite the shared Vue entry file.

- [ ] **Step 3: Compare Mail mapped Stitch states**

Compare `/inbox` and `/compose` against these Stitch screens:

```text
MMMail - Inbox
Inbox with Recovery Card
Inbox with Onboarding Progress
Compose - Minimized State
Compose - Normal State
Compose - Expanded State
First Compose with Green Lock
```

If a state cannot be produced locally, set capture status to `Blocked` and explain the missing fixture or interaction.

- [ ] **Step 4: Add Calendar rows**

Add rows for:

```text
/calendar
/calendar with edit drawer state
```

Use both duplicate `MMMail Calendar - Week View with Edit Drawer` Stitch resources as evidence. If only one visual differs materially, describe that difference in the Stitch inventory note.

- [ ] **Step 5: Add Drive rows**

Add rows for:

```text
/drive
/drive/shared
/drive/recent
/drive/starred
/drive/trash
```

Only `/drive` can be marked against `MMMail Drive - File Explorer` unless source inspection shows the same Stitch screen covers all sub-routes. Sub-routes without primary Stitch screens must be marked `Stitch design source missing`.

- [ ] **Step 6: Add Settings and onboarding guide rows**

Add rows for:

```text
/settings
First-login onboarding modal
Settings reopen entry for onboarding
```

Compare `/settings` against `Settings Workspace`. For the first-login onboarding modal, use `Inbox with Onboarding Progress` and setup/recovery screens as candidate evidence only if the visual pattern matches; otherwise mark the onboarding modal as `Stitch design source missing`.

- [ ] **Step 7: Add core-route findings to section 5**

For each divergence, add a severity row with evidence from at least two categories:

```text
Stitch screen identifier/name
Frontend route and Vue entry file
Browser screenshot path or capture note
Relevant source file and line reference
```

- [ ] **Step 8: Verify Task 6 output**

Run:

```bash
grep -n "Base shell desktop" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "| /inbox |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "| /calendar |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "| /settings |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
```

Expected: each command prints at least one matching line.

- [ ] **Step 9: Commit Task 6 audit report progress**

```bash
git add "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
git commit -m "docs: compare core frontend routes with stitch"
```

---

### Task 7: Compare product, governance, preview, Docs, and Sheets routes

**Files:**
- Modify: `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`
- Evidence: Stitch screens for Pass, Business, Suite, Security, Organizations, Retention Banners
- Evidence: screenshots in `.tmp/frontend-v2-stitch-conformance-2026-04-24/**`
- Read: product/governance/preview Vue view files from `frontend-v2/src/views/**`

- [ ] **Step 1: Add Pass rows**

Add rows for:

```text
/pass
/pass/shared-library
/pass/secure-links
/pass/alias-center
/pass/mailbox
/pass/business-policy
/pass/monitor
```

Compare `/pass` against `Pass Workspace` and `/pass/monitor` against `Pass Monitor`. Mark other Pass routes as `Stitch design source missing` unless source inspection shows the same Stitch screen covers the sub-route.

- [ ] **Step 2: Add Docs and Sheets rows**

Add rows for:

```text
/docs
/docs/:id
/sheets
/sheets/:id
```

These routes must be marked `Stitch design source missing` unless a primary Stitch screen in `projects/3947413193814542306` is found during screen inspection.

- [ ] **Step 3: Add Suite and Business rows**

Add rows for:

```text
/suite
/suite/plans
/suite/billing
/suite/operations
/suite/boundary
/business
```

Compare `/suite` against both Suite Overview screens and compare `/business` against `Business Overview`. Mark Suite sub-routes without matching primary Stitch screens as `Stitch design source missing`.

- [ ] **Step 4: Add Organizations rows**

Add rows for:

```text
/organizations
/organizations/members
/organizations/product-access
/organizations/domains
/organizations/mail-identities
/organizations/policy
/organizations/monitor
/organizations/session-monitor
/organizations/audit
```

Compare `/organizations/product-access` against `Organizations Product Access Matrix`. Mark other Organizations routes according to actual source and screen evidence.

- [ ] **Step 5: Add Security row**

Add a row for:

```text
/security
```

Compare it against `Security Center`.

- [ ] **Step 6: Add preview and research rows**

Add rows for:

```text
/collaboration
/command-center
/notifications
/labs
/labs/:moduleKey
```

These routes must be marked `Stitch design source missing` unless a primary Stitch screen is found during screen inspection.

- [ ] **Step 7: Assess Retention Banners Overview**

Search source for retention banner implementation:

```bash
grep -Rni "retention\|banner" "frontend-v2/src" --include="*.vue" --include="*.ts"
```

If a route/component is found, add it to the mapping table and conformance matrix. If no route/component is found, add a finding stating that Stitch has `Retention Banners Overview` but the current frontend implementation has no matching route or component evidence.

- [ ] **Step 8: Add product/governance/preview findings to section 5**

For each missing design source, record it separately from implementation divergence. Missing design source rows do not prove implementation defects; they prove the audit cannot mark the page as conforming.

- [ ] **Step 9: Verify Task 7 output**

Run:

```bash
grep -n "| /pass |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "| /docs |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "| /organizations/product-access |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "| /labs |" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
```

Expected: each command prints at least one matching line.

- [ ] **Step 10: Commit Task 7 audit report progress**

```bash
git add "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
git commit -m "docs: compare product and governance frontend routes with stitch"
```

---

### Task 8: Finalize findings, repair order, and recommendation

**Files:**
- Modify: `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`

- [ ] **Step 1: Write the executive summary**

The executive summary must answer these questions directly:

```text
Does frontend-v2 fully match the primary Stitch project?
Which route groups conform?
Which route groups diverge?
Which route groups lack a primary Stitch design source?
What blocks verification?
Should the project enter an implementation-fix phase?
```

If any route is `Partially conforms`, `Does not conform`, `Blocked`, `Not reachable`, or `Stitch design source missing`, the first answer must be `No, frontend-v2 cannot be considered fully conformant to Stitch yet.`

- [ ] **Step 2: Normalize the issue summary**

Section 5 must use this exact table:

```markdown
| Severity | Page/state | Finding | Evidence | Suggested fix direction |
| --- | --- | --- | --- | --- |
```

Sort rows by severity order: `P0`, `P1`, `P2`, `P3`.

- [ ] **Step 3: Normalize blocked and unverified items**

Section 6 must use this exact table:

```markdown
| Item | Why it could not be verified | Evidence attempted | Follow-up needed |
| --- | --- | --- | --- |
```

Include missing credentials, missing tokens, unavailable backend data, local dependency failures, and browser capture limitations.

- [ ] **Step 4: Write the recommended repair order**

Section 7 must group repairs in this order:

```text
1. P0 reachability or wrong-page issues.
2. P1 layout and information-architecture mismatches on mapped Stitch pages.
3. Missing primary Stitch design sources for reachable frontend routes.
4. P2 visual-system details on mapped pages.
5. P3 polish.
```

For each repair item, name the route, the Stitch screen if one exists, and the suggested fix direction.

- [ ] **Step 5: Write the implementation-fix phase recommendation**

Section 8 must use one of these exact recommendations:

```text
Recommendation: enter implementation-fix phase now.
Recommendation: do not enter implementation-fix phase until missing Stitch designs are resolved.
Recommendation: no implementation-fix phase is needed.
```

Use the first recommendation when mapped Stitch pages have P0/P1/P2 implementation divergences. Use the second recommendation when most gaps are missing primary Stitch designs. Use the third only if every reachable route either conforms or is intentionally outside the review with documented approval.

- [ ] **Step 6: Verify every page-level conclusion has enough evidence**

For each conformance matrix row, confirm at least two evidence categories are present:

```text
Stitch screen or variant identifier/name
Frontend route and Vue entry file
Browser screenshot path or capture note
Relevant source file and line reference
```

Rows with `Stitch design source missing` must still cite the frontend route and the absence of a primary Stitch screen.

- [ ] **Step 7: Verify Task 8 output**

Run:

```bash
grep -n "No, frontend-v2 cannot be considered fully conformant to Stitch yet\|fully conformant" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "Recommended repair order" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "Recommendation:" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
```

Expected: each command prints at least one matching line.

- [ ] **Step 8: Commit Task 8 audit report progress**

```bash
git add "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
git commit -m "docs: summarize frontend stitch conformance findings"
```

---

### Task 9: Final audit quality gate

**Files:**
- Modify: `docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`
- Read: `docs/superpowers/specs/2026-04-24-frontend-v2-stitch-conformance-design.md`

- [ ] **Step 1: Check required report sections exist**

Run:

```bash
grep -n "## Executive summary" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "## 1. Stitch project and design-source inventory" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "## 2. frontend-v2 route/page inventory" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "## 3. Stitch screen to frontend route mapping" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "## 4. Per-page conformance matrix" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "## 5. P0/P1/P2/P3 issue summary" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "## 6. Items that could not be verified" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "## 7. Recommended repair order" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
grep -n "## 8. Implementation-fix phase recommendation" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
```

Expected: every command prints a matching line.

- [ ] **Step 2: Check that the report does not claim conformance without a primary Stitch screen**

Run:

```bash
grep -n "Stitch design source missing.*Conforms\|Conforms.*Stitch design source missing" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md" || true
```

Expected: no output.

- [ ] **Step 3: Check that no historical assets are used as conformance sources**

Run:

```bash
grep -n "docs/assets" "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md" || true
```

Expected: no output, or only a sentence saying `docs/assets` was not used as a conformance source.

- [ ] **Step 4: Check that screenshots are not staged**

Run:

```bash
git status --short
```

Expected: no `.tmp/frontend-v2-stitch-conformance-2026-04-24/` screenshots are staged.

- [ ] **Step 5: Review the report against the approved spec**

Use the Read tool on:

```text
/home/xiang/桌面/project/MMMail-test/MMMail/docs/superpowers/specs/2026-04-24-frontend-v2-stitch-conformance-design.md
```

Confirm that the report satisfies acceptance criteria lines 164-171:

```text
Stitch MMMail project screens are inventoried.
Current frontend-v2 routes are inventoried.
Every reachable route is mapped to a Stitch screen or explicitly marked as missing design source.
Each mapped page has a conformance result and evidence.
P0/P1/P2/P3 findings are summarized with recommended repair order.
The report clearly states whether the implementation fully matches Stitch or which gaps remain.
```

- [ ] **Step 6: Commit final audit quality gate updates**

```bash
git add "docs/reports/frontend-v2-stitch-conformance-2026-04-24.md"
git commit -m "docs: finalize frontend stitch conformance audit"
```

- [ ] **Step 7: Report completion**

Return a concise completion note with:

```text
Report path: docs/reports/frontend-v2-stitch-conformance-2026-04-24.md
Primary Stitch project: projects/3947413193814542306
Screens inventoried: 27
Frontend route coverage: all routes from frontend-v2/src/app/router/routes.ts
Browser evidence: captured through gstack /browse, with blocked items listed in section 6
Final recommendation: quote section 8 exactly
```

---

## Plan Self-Review

- Spec coverage: The plan covers Stitch discovery, implementation discovery, browser capture through gstack `/browse`, mapping, comparison dimensions, severity model, report deliverable, evidence requirements, non-goals, risks, and acceptance criteria.
- Placeholder scan: The plan uses concrete project IDs, screen IDs, file paths, commands, table structures, status values, and severity values.
- Type/name consistency: The same report path, primary Stitch project, design-system asset, status vocabulary, severity vocabulary, and route groups are used throughout.
- Scope control: The plan produces an audit report only. It does not edit frontend implementation files, generate new Stitch designs, publish releases, or substitute historical screenshots for Stitch sources.
