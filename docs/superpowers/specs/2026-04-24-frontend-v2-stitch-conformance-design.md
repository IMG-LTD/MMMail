# Frontend v2 Stitch Conformance Review Design

## 1. Purpose

Review whether the current `frontend-v2` implementation fully conforms to the existing MMMail design screens in Stitch.

This is an audit and evidence-gathering task only. It does not redesign pages, edit Vue/CSS code, publish releases, or treat historical screenshots as the source of truth.

## 2. Design source of truth

Stitch is the only design source of truth for this review.

The review must use the existing MMMail project in Stitch:

1. Stitch MMMail project screens and variants
2. The design system attached to that Stitch project
3. Browser screenshots of the local frontend implementation, only as implementation evidence
4. `docs/assets` and historical UAT images, only as background context when helpful

A frontend page without a matching Stitch screen is marked as `Stitch design source missing`. It must not be judged as conforming by substituting `docs/assets`, generated mockups, or historical screenshots.

## 3. Scope

The review covers all reachable `frontend-v2` pages and states that can be mapped from the current route table and shell navigation.

Expected page groups include:

- Public routes: login, register, boundary, maintenance/offline/error pages, public share pages, story/onboarding surfaces
- Base shell: top bar, side navigation, mobile navigation, theme/locale/system shell behavior
- Core app routes: Suite, Mail/Inbox, Calendar, Drive, Settings
- Product surfaces: Pass, Docs, Sheets, Business, Organizations, Security
- Preview surfaces: Labs, Collaboration, Command Center, Notifications
- Recently added onboarding guide: first-login modal and Settings reopen entry

The route list must be generated from the current `frontend-v2/src/app/router/routes.ts` and related navigation files rather than from memory.

## 4. Review method

### 4.1 Stitch discovery

Use Stitch tools to list available projects, identify the MMMail project, and read its screens, variants, and design system.

For every design screen used in the audit, record:

- Stitch project identifier or name
- Screen identifier or name
- Variant name when applicable
- Relevant design-system tokens or component conventions when visible

### 4.2 Implementation discovery

Inspect the current frontend implementation:

- Route definitions in `frontend-v2/src/app/router/routes.ts`
- Shell/navigation definitions in `frontend-v2/src/layouts/modules/shell-nav.ts`
- Layouts in `frontend-v2/src/layouts/**`
- Views in `frontend-v2/src/views/**`
- Shared components/content/stores that materially affect the page

Build a route-to-component inventory before starting visual judgment.

### 4.3 Browser capture

Run the current frontend locally when possible and capture screenshots at fixed viewport sizes.

Minimum viewport:

- Desktop: `1440x900`

Additional viewports are used only when Stitch has responsive/mobile designs or when the route is shell/navigation-sensitive:

- Narrow desktop/tablet: `1024x768`
- Mobile: `390x844`

Project instruction requires browser work through gstack `/browse`; page opening and screenshots must follow that requirement.

### 4.4 Mapping

Create a mapping table between Stitch screens and frontend routes.

Each mapping row includes:

- Stitch screen / variant
- Frontend route
- Vue entry file and key child components
- Required auth/session/role state
- Capture status
- Mapping confidence: high, medium, low, or missing

If names do not match directly, record the mapping assumption and evidence.

## 5. Comparison dimensions

Each page is reviewed across these dimensions:

1. Layout structure: shell, sidebar, top bar, content grid, card hierarchy, empty regions
2. Visual system: color, typography scale, spacing, radius, border, shadow, density
3. Component states: empty, loading, error, success, disabled, active, selected
4. Content and i18n: key Chinese / Traditional Chinese / English labels and hierarchy
5. Interaction entry points: buttons, tabs, drawers, modals, menus, route transitions
6. Responsive behavior: only when Stitch provides responsive intent or shell behavior requires it
7. Design-system usage: whether implementation follows Stitch component/token intent where observable

## 6. Severity model

Findings use this severity scale:

- P0: page or critical flow is unreachable, or implementation is obviously not the same design
- P1: main layout, information architecture, or key component state significantly diverges
- P2: visual detail divergence such as spacing, color, density, icon treatment, or text hierarchy
- P3: minor polish issue or acceptable difference that should not block current use

Every non-conformance finding must include evidence and a suggested fix direction.

## 7. Deliverable

Create the audit report at:

`docs/reports/frontend-v2-stitch-conformance-2026-04-24.md`

The report must contain:

1. Stitch project and design-source inventory
2. `frontend-v2` route/page inventory
3. Stitch screen to frontend route mapping table
4. Per-page conformance matrix
5. P0/P1/P2/P3 issue summary
6. Items that could not be verified and why
7. Recommended repair order
8. Recommendation on whether to enter an implementation-fix phase

## 8. Evidence requirements

Every page-level conclusion must cite at least two of the following:

- Stitch screen or variant identifier/name
- Frontend route and Vue entry file
- Browser screenshot path or capture note
- Relevant source file and line reference

A page cannot be marked `conforms` unless a matching Stitch screen exists and the implementation was inspected.

## 9. Non-goals

This review will not:

- Generate new Stitch designs
- Replace missing Stitch screens with `docs/assets`
- Edit frontend implementation files
- Change product scope or support boundaries
- Publish a new version
- Treat source-level contract tests as proof of visual conformance

## 10. Risks and constraints

- Stitch screen naming may not map one-to-one to frontend route names. The report must make mapping confidence explicit.
- Some app routes may require authenticated or role-specific state. If fixture data or credentials are unavailable, those rows are marked as partially verified or blocked.
- The local dev environment may not have all dependencies installed. The audit plan must include setup/verification before screenshot capture.
- Browser/UI work must use gstack `/browse` per repository instructions.
- PowerShell, Docker, or backend services may be unavailable locally; the review should distinguish frontend visual blockers from infrastructure blockers.

## 11. Acceptance criteria

The audit is complete when:

- Stitch MMMail project screens are inventoried.
- Current `frontend-v2` routes are inventoried.
- Every reachable route is mapped to a Stitch screen or explicitly marked as missing design source.
- Each mapped page has a conformance result and evidence.
- P0/P1/P2/P3 findings are summarized with recommended repair order.
- The report clearly states whether the implementation fully matches Stitch or which gaps remain.
