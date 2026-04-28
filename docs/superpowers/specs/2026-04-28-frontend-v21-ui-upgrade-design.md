# Frontend v2.1 UI Upgrade Design

## 1. Purpose

MMMail v2.1 is a platform-level UI, interaction, routing, and product-capability upgrade. It replaces the current frontend visual direction with the new UI design image set under `docs/MMMail/UI`, and it plans the supporting frontend state, API contracts, backend capability boundaries, data-model boundaries, and open-source/commercial feature split required to make those designs real.

This design is a planning specification only. It does not implement frontend code, backend code, migrations, pricing, licensing changes, or deployment changes.

## 2. Decisions already confirmed

- v2.1 uses `docs/MMMail/UI` PNG designs as the highest visual and interaction source of truth.
- The previous Stitch v1.8 design source is deprecated for v2.1 planning and is not a v2.1 constraint.
- v2.1 is a full-scope plan covering all current `frontend-v2` routes plus all new pages, states, and capabilities implied by the UI designs.
- Complex capabilities visible in the designs are v2.1 real feature targets, not static UI shells.
- v2.1 may redesign route information architecture around the new UI. Existing internal route compatibility is not required.
- Product/system branding uses `MMMail`; organization and tenant names remain configurable data such as `Acme Corp` and are not hardcoded to MMMail.
- v2.1 must include Community open-source, Premium advanced, and Hosted commercial boundaries.
- v2.1 uses Vue 3, Vite, TypeScript, Vue Router, Pinia, Naive UI, a custom MMMail design system layer, and `vue-data-ui` for chart-heavy screens.
- The design documentation structure is one v2.1 master plan with a module sub-plan index. Detailed implementation plans can be created per module after this spec is approved.

## 3. Design source inventory

The UI source directory is `docs/MMMail/UI`. It contains PNG-only design assets grouped by product area:

| Folder | Product area | Role in v2.1 planning |
| --- | --- | --- |
| `首页` | Workspace / home dashboard | Primary source for `/workspace`, cross-product summary, quick actions, KPI cards, activity and right-panel patterns. |
| `邮件` | Mail | Primary source for mail shell, folders, message list, thread view, composer, security prompts, rules, shared mailbox, recovery states. |
| `日历` | Calendar | Primary source for calendar day/week/month views, event editor, conflict states, rooms, seats, resources, task-calendar mixed surfaces. |
| `云盘` | Drive | Primary source for file explorer, upload queue, preview, sharing, permissions, versions, storage monitoring. |
| `文档` | Docs | Primary source for document workspace, editor, templates, sharing, permission matrix, comments, version history and diff. |
| `Sheets和labs` | Sheets and Labs | Primary source for spreadsheet grid, imports, data cleaning, insights, AI/Labs side panel and analytics views. |
| `Pass` | Pass | Primary source for vaults, shared library, secure links, aliases, policies, import wizard, monitor, risk dashboard. |
| `Collaboration` | Collaboration | Primary source for projects, tasks, kanban, comments, knowledge, activity, schedule views. |
| `CommandCenter` | Command Center | Primary source for commands, run details, workflow canvas, streaming logs, SLO analytics, audit. |
| `Notifications` | Notifications | Primary source for inbox, rules, subscriptions, templates, composer, analytics, channel previews. |
| `Admin` | Admin / Governance | Primary source for admin dashboard, users, roles, domains, policies, audit, alerts, billing, integrations, system, risk. |
| `Setting` | Settings | Primary source for profile, security, devices, notification preferences, privacy, integrations, storage, billing, audit, help. |

All visible placeholder names and logos in these images are illustrative. v2.1 replaces them with MMMail product branding and configurable organization data.

## 4. Scope

### 4.1 In scope

- New v2.1 route IA across public, auth, share, workspace, product, governance, settings, labs, and system pages.
- Unified App Shell and responsive navigation.
- MMMail design tokens and Naive UI theme overrides.
- Shared components for cards, tables, forms, drawers, modals, badges, charts, upload queues, empty states, error states, command palette, and gates.
- Module-level page and state planning for Workspace, Mail, Calendar, Drive, Docs, Sheets, Pass, Collaboration, Command Center, Notifications, Admin/Governance, Settings, Labs, public/auth/share/system routes.
- Real feature targets for design-implied capabilities such as room booking, seat booking, Drive preview, Docs version diff, Sheets data cleaning and AI insights, Pass enterprise policy, Command Center workflow canvas, notification composer, Admin risk policies.
- Frontend state boundaries, API contracts, backend capability boundaries, and data-model boundaries.
- Community/Premium/Hosted entitlement boundaries and UI expression.
- Responsive behavior and accessibility rules.
- Testing and browser validation strategy.

### 4.2 Out of scope for this design document

- Implementing frontend components or routes.
- Implementing backend endpoints or database migrations.
- Writing pricing copy, license text, or billing provider integration.
- Publishing a release.
- Treating old Stitch screens as v2.1 conformance sources.

## 5. Product and branding rules

| Element | v2.1 rule |
| --- | --- |
| Product name | `MMMail` |
| Platform label | `MMMail Workspace` or `MMMail Suite` where a platform label is needed. |
| Organization name | Configurable tenant/workspace data such as `Acme Corp`; never hardcoded to `MMMail`. |
| Placeholder brand names | `Nexa Workspace`, `Nova Workspace`, `New Workspace`, `Labs Pro`, and similar design placeholders must not appear in final UI. |
| Logo | Use the project-owned MMMail logo or generated MMMail brand mark. Design image logos are examples only. |
| Sub-product naming | MMMail Mail, MMMail Calendar, MMMail Drive, MMMail Docs, MMMail Sheets, MMMail Pass, MMMail Command Center, MMMail Admin, MMMail Labs. |

## 6. Community, Premium, and Hosted boundaries

MMMail v2.1 should be designed as an open-source product with optional premium and hosted capabilities. The UI must avoid implying that paid or hosted-only capabilities are fully included in the Community self-hosted edition.

### 6.1 Community open-source edition

Community focuses on self-hosted adoption and a usable collaboration baseline:

- Workspace dashboard.
- Mail baseline: folders, conversation list/detail, composer, drafts, labels, basic search.
- Calendar baseline: day/week/month views, event create/edit, reminders.
- Drive baseline: files, folders, upload/download, basic preview, basic share.
- Docs baseline: document list, templates, editor, basic comments, basic share.
- Sheets baseline: sheet list, editor, tabs, basic import.
- Pass baseline: personal vault, item create/edit, basic security score.
- Collaboration baseline: projects, tasks, comments, activity.
- Notifications baseline: inbox and basic preferences.
- Settings baseline: profile, security, devices, privacy, integrations, help.
- Security baseline: account sessions, device visibility, recovery cues.
- Organizations/Admin baseline: users, organizations, domains, basic policy, basic audit, system health.
- Public share access.
- Self-hosted install, upgrade, backup, restore, and health checks.

### 6.2 Premium advanced edition

Premium focuses on advanced team, enterprise, security, automation, and AI capabilities:

- Advanced Admin governance, risk policy, compliance reporting, alert rules, audit export.
- Command Center execution, batch runs, workflow canvas, streaming logs, SLO analytics.
- Notification rules, templates, multi-channel orchestration, composer, delivery analytics, webhook testing.
- Pass shared library approvals, secure links, aliases, mailbox routing, enterprise policies, risk monitoring, breach monitoring.
- Drive advanced preview, version audit, approval flows, storage policy, access analytics, security scan.
- Calendar room booking, seat booking, resource scheduling, availability optimization, organization scheduling policy.
- Docs version diff, approvals, permission matrix, advanced collaboration audit.
- Sheets data cleaning, AI insights, chart generation, external data source sync.
- Labs and AI-enhanced features.
- Billing plans, quota, invoices, commercial reconciliation, advanced usage analytics.

### 6.3 Hosted commercial capabilities

Hosted capabilities are operational/commercial services and must not be described as included self-hosted features:

- Managed backups.
- Managed monitoring.
- SLA.
- Multi-tenant commercial billing.
- Hosted object storage.
- HA deployment operations.
- Enterprise support.
- Compliance report generation.

### 6.4 UI boundary expression

Every gated feature needs consistent UI language:

- `PremiumBadge` for paid advanced capabilities.
- `HostedBadge` for hosted-only capabilities.
- `MaturityBadge` for GA/Beta/Preview status.
- `PermissionGate` for role-based restrictions.
- `PremiumGate` for paid feature restrictions.
- Upgrade CTA for Premium features.
- Contact admin CTA when the user lacks permission but the workspace owns the entitlement.
- Self-hosted limitation text for hosted-only services.
- Disabled state with tooltip or inline explanation.
- No hidden bait-and-switch: if a feature is paid, the list/card/action must say so before the user starts a flow.

## 7. Route information architecture

v2.1 can redesign route IA around the new UI. Legacy internal URLs do not constrain the v2.1 design. Temporary redirects can be considered during implementation, but the v2.1 route map is the product target.

### 7.1 Public, auth, share, and system routes

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

`/` routes to `/workspace` after session resolution. Public share routes stay public and token-scoped.

### 7.2 Workspace

```text
/workspace
/workspace/today
/workspace/activity
/workspace/tasks
```

### 7.3 Mail

```text
/mail
/mail/inbox
/mail/starred
/mail/snoozed
/mail/drafts
/mail/scheduled
/mail/outbox
/mail/sent
/mail/archive
/mail/spam
/mail/trash
/mail/unread
/mail/contacts
/mail/search
/mail/folders/:folderId
/mail/labels/:labelId
/mail/conversations/:threadId
/mail/settings
/mail/compose
```

Composer and reply states can also be expressed through query state:

```text
/mail/inbox?compose=new
/mail/conversations/:threadId?reply=1
/mail/conversations/:threadId?panel=security
```

### 7.4 Calendar

```text
/calendar
/calendar/day
/calendar/week
/calendar/month
/calendar/rooms
/calendar/seats
/calendar/resources
/calendar/settings
```

Panel states:

```text
/calendar/week?modal=create-event
/calendar/week?event=:eventId
/calendar/rooms?panel=booking
```

### 7.5 Drive

```text
/drive
/drive/recent
/drive/shared
/drive/starred
/drive/trash
/drive/folders/:folderId
/drive/files/:fileId
/drive/uploads
/drive/storage
/drive/admin
```

Panel states:

```text
/drive/files/:fileId?panel=preview
/drive/files/:fileId?panel=sharing
/drive?drawer=upload-queue
```

### 7.6 Docs

```text
/docs
/docs/templates
/docs/:documentId
/docs/:documentId/versions
/docs/:documentId/share
```

### 7.7 Sheets and Labs

```text
/sheets
/sheets/import
/sheets/:sheetId
/sheets/:sheetId/data-cleaning
/sheets/:sheetId/insights

/labs
/labs/:moduleKey
/labs/:moduleKey/settings
```

### 7.8 Pass

```text
/pass
/pass/vault
/pass/vaults/:vaultId
/pass/items/:itemId
/pass/shared
/pass/secure-links
/pass/aliases
/pass/mailbox
/pass/policies
/pass/import
/pass/monitor
```

### 7.9 Collaboration

```text
/collaboration
/collaboration/projects
/collaboration/projects/:projectId
/collaboration/tasks
/collaboration/tasks/:taskId
/collaboration/knowledge
/collaboration/activity
```

### 7.10 Command Center

```text
/command-center
/command-center/commands
/command-center/commands/:commandId
/command-center/runs
/command-center/runs/:runId
/command-center/workflows
/command-center/workflows/:workflowId
/command-center/audit
```

### 7.11 Notifications

```text
/notifications
/notifications/inbox
/notifications/rules
/notifications/subscriptions
/notifications/templates
/notifications/compose
/notifications/analytics
```

### 7.12 Admin and Governance

```text
/admin
/admin/users
/admin/roles
/admin/organizations
/admin/domains
/admin/policies
/admin/audit
/admin/alerts
/admin/integrations
/admin/billing
/admin/system
/admin/risk
```

### 7.13 Settings

```text
/settings
/settings/profile
/settings/security
/settings/devices
/settings/notifications
/settings/privacy
/settings/integrations
/settings/storage
/settings/billing
/settings/audit
/settings/help
```

### 7.14 Current frontend-v2 capability replacement map

Route compatibility is not required, but existing user-facing capabilities need an explicit v2.1 destination.

| Current capability group | v2.1 replacement |
| --- | --- |
| Root, login, register, boundary, product-access-blocked, offline, maintenance, 404/500, failure modes | Public/auth/share/system routes in section 7.1. |
| Public mail, Drive, and Pass share links | Token-scoped share routes in section 7.1. |
| Suite and workspace dashboard | `/workspace`, `/workspace/today`, `/workspace/activity`, `/workspace/tasks`. |
| Mail and Inbox routes | Mail route family in section 7.3, including folders, labels, search, conversations, composer, and settings. |
| Calendar routes | Calendar route family in section 7.4, including rooms, seats, resources, event panels, and settings. |
| Drive routes | Drive route family in section 7.5, including files, folders, uploads, preview, sharing, storage, and admin. |
| Pass, Docs, and Sheets product routes | Product route families in sections 7.6, 7.7, and 7.8. |
| Business, Organizations, and Security governance routes | Admin/Governance and Settings destinations in sections 7.12 and 7.13. Business/billing moves to `/admin/billing` and `/settings/billing`; organizations move to `/admin/organizations`; security moves to `/settings/security`, `/admin/policies`, `/admin/audit`, and `/admin/risk`. |
| Collaboration, Command Center, Notifications, and Labs preview routes | Promoted to first-class v2.1 modules in sections 7.7, 7.9, 7.10, and 7.11. |
| First-login guide and story/onboarding surfaces | `/onboarding/:storyKey`, plus Settings/Help reopen entry and shell-triggered onboarding state. |

## 8. Visual system

### 8.1 Visual direction

v2.1 follows the UI asset set's modern B2B collaboration console style:

- Light gray application background.
- White main surfaces.
- Fine borders.
- Light shadows.
- 10–14px rounded cards.
- High-density but readable layouts.
- Teal/green primary action color.
- Blue, orange, red, purple status and product accent colors.
- Context panels and drawers as the main carrier for complex secondary information.
- Shared visual language for cards, tables, lists, charts, activity streams, modals, and drawers.

### 8.2 Design tokens

| Token group | Required tokens |
| --- | --- |
| Background | `app-bg`, `surface`, `surface-soft`, `surface-muted`, `overlay` |
| Borders | `border`, `border-strong`, `focus-ring` |
| Text | `text-primary`, `text-secondary`, `text-muted`, `text-disabled`, `text-inverse` |
| Brand | `brand-primary`, `brand-primary-hover`, `brand-soft`, `brand-border`, `brand-contrast` |
| Status | `success`, `info`, `warning`, `danger`, `premium`, `hosted`, `preview` |
| Product accents | mail, calendar, drive, docs, sheets, pass, collaboration, command, notifications, admin, settings, labs |
| Radius | `radius-xs`, `radius-sm`, `radius-md`, `radius-lg`, `radius-xl` |
| Shadow | `shadow-none`, `shadow-sm`, `shadow-md`, `shadow-lg` |
| Spacing | 4px base grid with 8, 12, 16, 24, 32px common values |
| Motion | `duration-fast`, `duration-base`, `duration-slow`, easing tokens, reduced-motion behavior |

### 8.3 Color and contrast

- Primary actions use teal/green with sufficient contrast against white and light gray backgrounds.
- Risk and destructive actions use red plus text labels, never color alone.
- Premium and hosted states use explicit badges and copy, not only purple/gold styling.
- Disabled states must remain legible.
- Text contrast targets WCAG AA for normal text and interactive controls.
- Dark mode remains supported through token mapping, but light mode is the v2.1 visual baseline because the design image set is mostly light-mode.

## 9. App Shell and navigation

### 9.1 Shell structure

```text
AppShell
├─ GlobalTopBar
├─ ProductSideNav
├─ ProductSubNav
├─ MainContent
├─ ContextPanel
└─ MobileTabBar
```

### 9.2 GlobalTopBar

Responsibilities:

- Global search.
- Workspace/organization switcher.
- Quick create button.
- Notifications entry.
- Help entry.
- User avatar/menu.
- Theme and locale entry.

Behavior:

- Search focus opens a command palette.
- Quick create menu adapts to the active product.
- Notifications entry opens notification drawer.
- Workspace switcher shows tenant/workspace options and entitlement state.

### 9.3 ProductSideNav

Primary entries:

```text
Workspace
Mail
Calendar
Drive
Docs
Sheets
Pass
Collaboration
Command Center
Notifications
Admin
Settings
Labs
```

Behavior:

- Collapsible on desktop.
- Current product highlighted.
- Premium and Preview entries show badges.
- Mobile uses bottom tabs plus a More panel.

### 9.4 ContextPanel

The right context panel is a core v2.1 interaction pattern.

Used for:

- Details.
- Activity stream.
- Comments.
- Permissions.
- Risk and security insights.
- AI suggestions.
- Audit history.
- Approval state.

Responsive behavior:

| Viewport | ContextPanel behavior |
| --- | --- |
| Desktop >= 1280px | Fixed or collapsible right rail. |
| Tablet 768–1279px | Drawer. |
| Mobile < 768px | Full-screen sheet. |

## 10. Shared component system

v2.1 needs a custom MMMail design system layer above Naive UI. The implementation should not expose raw Naive defaults as the final product look.

### 10.1 Required shared components

```text
PageHeader
SectionHeader
MetricTile
KpiCard
ActionCard
DataTable
DataGrid
StatusBadge
MaturityBadge
PremiumBadge
HostedBadge
EmptyState
ErrorState
LoadingSkeleton
SearchInput
FilterBar
SegmentedTabs
Breadcrumb
Pagination
AvatarStack
Timeline
ActivityFeed
CommentThread
PermissionMatrix
RoleSelector
ConfirmDialog
FormDrawer
InspectorDrawer
UploadQueue
ProgressToast
ChartCard
TerminalLog
CommandPalette
PermissionGate
PremiumGate
MaturityGate
ProductAccessGate
```

### 10.2 DataTable behavior

- 44–56px row height.
- Sticky header.
- Sorting.
- Filtering.
- Batch selection.
- Inline actions.
- Row hover and selected states.
- Right-side detail panel integration.
- Empty, loading, error, permission-denied, and premium-locked states.
- Horizontal scroll for dense desktop tables.
- Card conversion for mobile.

### 10.3 Forms, modals, and drawers

Forms:

- Label above field.
- Required indicators.
- Field-level errors.
- Async validation loading state.
- Grouped long forms.
- Fixed submit/cancel footer in drawers.
- Inline success and error feedback.

Modals:

- Destructive confirmation.
- Small create flow.
- Authorization confirmation.
- Upgrade prompt.
- High-risk action confirmation.

Drawers:

- Complex create/edit flows.
- File sharing.
- Event editor.
- Permission matrix.
- Command parameters.
- Notification rules.
- Detail inspection.

Popovers:

- Quick menus.
- Date and member pickers.
- Status changes.
- Batch action menus.

### 10.4 Motion

Required motion patterns:

- 150–220ms local transitions.
- Drawer slide-in.
- Modal fade plus scale.
- Popover fade.
- Row hover transition.
- Tab underline transition.
- Upload progress.
- Command run streaming log.
- Toast enter/exit.
- Skeleton shimmer.
- Drag-and-drop placeholder.
- Calendar event drag ghost.
- Kanban card drag ghost.

Accessibility:

- Respect `prefers-reduced-motion`.
- Modal and drawer focus trapping.
- Escape closes non-destructive overlays.
- Toast and background jobs use `aria-live`.
- Keyboard navigation for tables, menus, tabs, and drawers.

## 11. Responsive and adaptive behavior

| Breakpoint | Behavior |
| --- | --- |
| Desktop >= 1280px | Left nav + top bar + main content + right context panel. |
| Tablet 768–1279px | Collapsible left nav, right context drawer, tables keep horizontal scroll. |
| Mobile < 768px | Bottom tabs, More panel, single-column content, tables become cards, details become full-screen sheet. |

Every module must define:

- Desktop layout.
- Tablet layout.
- Mobile layout.
- Empty state.
- Loading state.
- Error state.
- Permission denied state.
- Premium locked state.
- High-risk confirmation behavior.

## 12. Frontend implementation architecture

### 12.1 Technology stack

```text
Vue 3
Vite
TypeScript
Vue Router
Pinia
Naive UI
MMMail custom design system layer
vue-data-ui for chart-heavy dashboards
@vueuse/core
CSS variables and tokenized global styles
```

`vue-data-ui` is preferred for:

- Workspace KPI trends.
- Drive storage and file-type distribution.
- Sheets insights and data-cleaning analytics.
- Pass risk and safety charts.
- Command Center SLO and run analytics.
- Notifications delivery analytics.
- Admin health, billing, audit, and risk dashboards.

It is not the default solution for rich document editing, spreadsheet grid editing, command workflow canvas, or file previews. Those need separate module-specific implementation choices.

### 12.2 Suggested frontend structure

```text
frontend-v2/src/
├─ app/
│  ├─ router/
│  ├─ providers/
│  └─ App.vue
├─ shell/
├─ design-system/
│  ├─ tokens/
│  ├─ components/
│  └─ naive-theme.ts
├─ features/
│  ├─ workspace/
│  ├─ mail/
│  ├─ calendar/
│  ├─ drive/
│  ├─ docs/
│  ├─ sheets/
│  ├─ pass/
│  ├─ collaboration/
│  ├─ command-center/
│  ├─ notifications/
│  ├─ admin/
│  ├─ settings/
│  └─ labs/
├─ shared/
│  ├─ api/
│  ├─ auth/
│  ├─ billing/
│  ├─ permissions/
│  ├─ i18n/
│  ├─ motion/
│  └─ utils/
└─ styles/
```

### 12.3 State strategy

Use Pinia stores and a shared API/query layer.

Global stores:

```text
session
workspace
shell
routePanels
billing
permissions
premium
backgroundJobs
uploadQueue
notificationStatus
```

Module stores:

```text
mail
calendar
drive
docs
sheets
pass
collaboration
commandCenter
notifications
admin
settings
labs
```

Key shared state:

- Current session.
- Current workspace and organization.
- Active product.
- Current theme and locale.
- Active panel/modal/drawer.
- Permissions.
- Entitlements.
- Background jobs.
- Upload queue.
- Notification unread count.

## 13. API and data-model boundaries

### 13.1 API namespaces

```text
/api/v2/workspace/*
/api/v2/mail/*
/api/v2/calendar/*
/api/v2/drive/*
/api/v2/docs/*
/api/v2/sheets/*
/api/v2/pass/*
/api/v2/collaboration/*
/api/v2/command-center/*
/api/v2/notifications/*
/api/v2/admin/*
/api/v2/settings/*
/api/v2/labs/*
/api/v2/billing/*
/api/v2/entitlements/*
```

### 13.2 Shared response model

```ts
type ApiResponse<T> = {
  data: T
  meta?: {
    requestId: string
    pagination?: PaginationMeta
    warnings?: ApiWarning[]
  }
}
```

### 13.3 Shared error model

```ts
type ApiError = {
  code: string
  message: string
  fieldErrors?: Record<string, string[]>
  retryable?: boolean
  upgradeRequired?: boolean
  requiredPlan?: 'premium' | 'hosted'
}
```

Paid/hosted restrictions should return either HTTP 402 or HTTP 403 with `upgradeRequired = true`, depending on backend policy. The frontend must show the same Premium/Hosted locked pattern either way.

### 13.4 Permission model

```ts
type Permission =
  | 'mail:read'
  | 'mail:write'
  | 'drive:share'
  | 'admin:users:manage'
  | 'command:runs:execute'
  | 'billing:manage'
```

### 13.5 Entitlement model

```ts
type Entitlement =
  | 'community'
  | 'premium'
  | 'hosted'
  | 'labs-ai'
  | 'enterprise-governance'
  | 'advanced-security'
```

## 14. Module-level design

### 14.1 Workspace

Routes:

```text
/workspace
/workspace/today
/workspace/activity
/workspace/tasks
```

Primary UI source: `docs/MMMail/UI/首页`.

Core UI:

- Dashboard summary.
- Recent mail.
- Upcoming calendar events.
- Tasks.
- File activity.
- System status.
- Notifications and recommendation right panel.

API boundary:

```text
GET /api/v2/workspace/summary
GET /api/v2/workspace/activity
GET /api/v2/workspace/tasks
PATCH /api/v2/workspace/tasks/:id
```

Community: summary, activity, basic tasks, module entry cards.

Premium: cross-module smart recommendations, organization-level risk and operating insights.

### 14.2 Mail

Routes: all routes in section 7.3.

Primary UI source: `docs/MMMail/UI/邮件`.

Core UI:

- Folder and label navigation.
- Message list.
- Thread reader.
- Composer drawer.
- Attachment preview.
- Mail security panel.
- Rules and shared mailbox settings.

API boundary:

```text
GET /api/v2/mail/folders
GET /api/v2/mail/messages
GET /api/v2/mail/threads/:id
POST /api/v2/mail/drafts
PATCH /api/v2/mail/drafts/:id
POST /api/v2/mail/send
POST /api/v2/mail/messages/bulk-action
GET /api/v2/mail/contacts
GET /api/v2/mail/rules
POST /api/v2/mail/rules
```

Community: folders, messages, conversations, composer, drafts, labels, basic search.

Premium: shared mailbox, advanced rules, security scan, compliance archive, send approval, organization audit.

### 14.3 Calendar

Routes: all routes in section 7.4.

Primary UI source: `docs/MMMail/UI/日历`.

Core UI:

- Day/week/month grid.
- Event editor drawer.
- Event detail popover.
- Availability panel.
- Room booking.
- Seat booking.
- Resource schedule.

API boundary:

```text
GET /api/v2/calendar/events
POST /api/v2/calendar/events
PATCH /api/v2/calendar/events/:id
DELETE /api/v2/calendar/events/:id
GET /api/v2/calendar/availability
GET /api/v2/calendar/resources
POST /api/v2/calendar/bookings
GET /api/v2/calendar/settings
PATCH /api/v2/calendar/settings
```

Community: basic calendar and event management.

Premium: rooms, seats, resources, availability optimization, organization policy.

### 14.4 Drive

Routes: all routes in section 7.5.

Primary UI source: `docs/MMMail/UI/云盘`.

Core UI:

- Folder tree.
- File table/grid.
- Upload queue.
- File preview drawer.
- Sharing and permissions drawer.
- Version history.
- Storage dashboard.

API boundary:

```text
GET /api/v2/drive/folders
GET /api/v2/drive/files
POST /api/v2/drive/uploads
GET /api/v2/drive/uploads/:id
PATCH /api/v2/drive/files/:id
DELETE /api/v2/drive/files/:id
POST /api/v2/drive/files/:id/share
GET /api/v2/drive/files/:id/versions
GET /api/v2/drive/storage/summary
```

Community: files, folders, upload/download, basic share and preview.

Premium: advanced preview, version audit, approval flow, storage policy, analytics, security scan.

### 14.5 Docs

Routes: all routes in section 7.6.

Primary UI source: `docs/MMMail/UI/文档`.

Core UI:

- Document workspace.
- Template picker.
- Editor.
- Outline.
- Comments.
- Share permissions.
- Version history and diff.

API boundary:

```text
GET /api/v2/docs
POST /api/v2/docs
GET /api/v2/docs/:id
PATCH /api/v2/docs/:id
GET /api/v2/docs/:id/comments
POST /api/v2/docs/:id/comments
GET /api/v2/docs/:id/versions
POST /api/v2/docs/:id/share
```

Community: documents, templates, basic comments, basic share.

Premium: version diff, approvals, permission matrix, collaboration audit.

### 14.6 Sheets and Labs

Routes: all routes in section 7.7.

Primary UI source: `docs/MMMail/UI/Sheets和labs`.

Core UI:

- Sheets workspace.
- Spreadsheet editor.
- Formula bar.
- Sheet tabs.
- Import wizard.
- Data cleaning rules.
- AI insights panel.
- Labs module configuration.

API boundary:

```text
GET /api/v2/sheets
POST /api/v2/sheets
GET /api/v2/sheets/:id
PATCH /api/v2/sheets/:id
POST /api/v2/sheets/:id/imports
POST /api/v2/sheets/:id/cleaning-rules
GET /api/v2/sheets/:id/insights
GET /api/v2/labs/modules
GET /api/v2/labs/modules/:key
PATCH /api/v2/labs/modules/:key/settings
```

Community: basic sheets, tabs, basic import.

Premium: data cleaning, AI analysis, charts, external data sync, advanced Labs modules.

### 14.7 Pass

Routes: all routes in section 7.8.

Primary UI source: `docs/MMMail/UI/Pass`.

Core UI:

- Personal vault.
- Shared library.
- Secure links.
- Aliases and mailbox routing.
- Enterprise policies.
- Import wizard.
- Monitor dashboard.

API boundary:

```text
GET /api/v2/pass/vaults
GET /api/v2/pass/items
POST /api/v2/pass/items
PATCH /api/v2/pass/items/:id
POST /api/v2/pass/share
GET /api/v2/pass/secure-links
POST /api/v2/pass/secure-links
DELETE /api/v2/pass/secure-links/:id
GET /api/v2/pass/aliases
PATCH /api/v2/pass/aliases/:id
GET /api/v2/pass/monitor
```

Community: basic vault, item management, basic security score.

Premium: shared library approval, enterprise policy, secure links, aliases, risk monitoring, breach monitoring.

### 14.8 Collaboration

Routes: all routes in section 7.9.

Primary UI source: `docs/MMMail/UI/Collaboration`.

Core UI:

- Project list.
- Kanban board.
- Task detail drawer.
- Comments.
- Attachments.
- Knowledge area.
- Activity stream.
- Schedule view.

API boundary:

```text
GET /api/v2/collaboration/projects
POST /api/v2/collaboration/projects
GET /api/v2/collaboration/projects/:id
GET /api/v2/collaboration/tasks
POST /api/v2/collaboration/tasks
PATCH /api/v2/collaboration/tasks/:id
POST /api/v2/collaboration/tasks/:id/comments
GET /api/v2/collaboration/activity
```

Community: projects, tasks, comments, activity.

Premium: advanced approvals, automation, AI summaries, organization collaboration analytics.

### 14.9 Command Center

Routes: all routes in section 7.10.

Primary UI source: `docs/MMMail/UI/CommandCenter`.

Core UI:

- Command templates.
- Parameter forms.
- Run history.
- Streaming run log.
- Cancel and retry actions.
- Batch runs.
- Workflow canvas.
- Audit.

API boundary:

```text
GET /api/v2/command-center/commands
GET /api/v2/command-center/commands/:id
POST /api/v2/command-center/runs
GET /api/v2/command-center/runs/:id
POST /api/v2/command-center/runs/:id/cancel
POST /api/v2/command-center/runs/:id/retry
GET /api/v2/command-center/workflows
POST /api/v2/command-center/workflows
GET /api/v2/command-center/audit
```

Community: command templates and execution history.

Premium: real-time execution, batch runs, workflow canvas, audit export, SLO analytics.

### 14.10 Notifications

Routes: all routes in section 7.11.

Primary UI source: `docs/MMMail/UI/Notifications`.

Core UI:

- Notification inbox.
- Rules.
- Subscriptions.
- Templates.
- Composer.
- Channel preview.
- Analytics.

API boundary:

```text
GET /api/v2/notifications
PATCH /api/v2/notifications/:id
GET /api/v2/notifications/rules
POST /api/v2/notifications/rules
GET /api/v2/notifications/subscriptions
PATCH /api/v2/notifications/subscriptions/:id
GET /api/v2/notifications/templates
POST /api/v2/notifications/send
GET /api/v2/notifications/analytics
```

Community: inbox and basic preferences.

Premium: rules, templates, multi-channel sending, analytics, webhooks.

### 14.11 Admin and Governance

Routes: all routes in section 7.12.

Primary UI source: `docs/MMMail/UI/Admin`.

Core UI:

- Admin overview.
- Users.
- Roles.
- Organizations.
- Domains.
- Policies.
- Audit.
- Alerts.
- Integrations.
- Billing.
- System health.
- Risk.

API boundary:

```text
GET /api/v2/admin/summary
GET /api/v2/admin/users
POST /api/v2/admin/users
PATCH /api/v2/admin/users/:id
GET /api/v2/admin/roles
GET /api/v2/admin/domains
GET /api/v2/admin/policies
PATCH /api/v2/admin/policies/:id
GET /api/v2/admin/audit
GET /api/v2/admin/alerts
GET /api/v2/admin/system
GET /api/v2/admin/risk
```

Community: users, organizations, domains, basic policy, basic audit, basic system health.

Premium: advanced governance, risk policy, compliance reporting, commercial billing, tenant policy, alert rules, SLA monitoring.

### 14.12 Settings

Routes: all routes in section 7.13.

Primary UI source: `docs/MMMail/UI/Setting`.

Core UI:

- Profile.
- Security.
- Devices.
- Notifications.
- Privacy.
- Integrations.
- Storage.
- Billing.
- Audit.
- Help.

API boundary:

```text
GET /api/v2/settings/profile
PATCH /api/v2/settings/profile
GET /api/v2/settings/security
PATCH /api/v2/settings/security
GET /api/v2/settings/devices
DELETE /api/v2/settings/devices/:id
GET /api/v2/settings/notifications
PATCH /api/v2/settings/notifications
GET /api/v2/settings/integrations
PATCH /api/v2/settings/integrations/:id
GET /api/v2/settings/audit
```

Community: profile, security, devices, notifications, privacy, basic integrations, help.

Premium: advanced integrations, billing, storage policy, audit export, enterprise security policy.

### 14.13 Public, auth, share, and system pages

Routes: all routes in section 7.1.

Core UI:

- Login.
- Register.
- Boundary and product access blocked pages.
- Public share pages for mail, drive, and pass.
- Onboarding stories.
- Failure modes.
- Error/offline/maintenance pages.

API boundary:

```text
POST /api/v2/auth/login
POST /api/v2/auth/register
GET /api/v2/share/mail/:token
GET /api/v2/share/drive/:token
GET /api/v2/share/pass/:token
GET /api/v2/system/status
```

Community: all public/auth/share/system routes required for baseline operation.

Premium: Premium gates can appear on product access blocked pages and share advanced controls, but public access itself must not require a paid UI shell.

## 15. Module sub-plan index

After this master spec is approved, implementation planning should be split into focused module plans:

1. `frontend-v21-design-system-and-shell`
2. `frontend-v21-routing-and-gates`
3. `frontend-v21-public-auth-share-system`
4. `frontend-v21-workspace-mail-calendar-drive-settings`
5. `frontend-v21-docs-sheets-labs-collaboration`
6. `frontend-v21-pass-notifications-command-center`
7. `frontend-v21-admin-governance-billing-entitlements`
8. `backend-v21-api-contracts-and-data-models`
9. `frontend-v21-responsive-accessibility-visual-qa`

Each module plan should include tests, route coverage, component coverage, API contract coverage, and browser validation.

## 16. Testing strategy

### 16.1 Contract tests

- v2.1 route table contains every planned route.
- Every route has product area, maturity, entitlement, permission, and layout metadata.
- Public routes do not require authenticated session.
- Premium routes use `PremiumGate` or route-level entitlement metadata.
- Hosted-only features use `HostedBadge` and hosted limitation copy.
- Each module declares empty, loading, error, permission denied, premium locked, and normal states.

### 16.2 Component tests

Required components:

- DataTable.
- DataGrid.
- Drawer.
- Modal.
- PremiumGate.
- PermissionGate.
- UploadQueue.
- CommandPalette.
- StatusBadge.
- EmptyState.
- ErrorState.
- ChartCard.
- TerminalLog.

### 16.3 Accessibility tests

- Modal and drawer focus trapping.
- Escape behavior.
- Keyboard navigation for nav, tables, tabs, menus, drawers.
- `aria-live` for toasts, uploads, command runs, background jobs.
- `prefers-reduced-motion` support.
- Color contrast for text, badges, buttons, alerts.
- Status not conveyed by color alone.

### 16.4 Browser visual validation

Browser validation must follow repository instructions and use gstack `/browse`.

Minimum viewports:

```text
Desktop: 1440x900
Tablet: 1024x768
Mobile: 390x844
```

Validate:

- App Shell.
- Top bar.
- Side nav.
- Mobile nav.
- Context panel.
- Tables.
- Drawers.
- Modals.
- Premium locked states.
- Permission denied states.
- Empty states.
- Error states.
- Loading states.
- Major module dashboards and editors.

## 17. Acceptance criteria

The v2.1 design and implementation are complete when:

- Every design image group under `docs/MMMail/UI` is mapped to routes, states, panels, dialogs, or module capabilities.
- Every current `frontend-v2` route capability has a v2.1 replacement route or documented replacement state.
- Public/auth/share/system routes are preserved as product boundaries.
- The v2.1 route IA is implemented with route metadata for layout, maturity, permissions, and entitlements.
- All visible placeholder design brands are replaced with MMMail product branding or configurable tenant data.
- Community, Premium, and Hosted boundaries are visible and consistent across routes and components.
- Naive UI is themed through MMMail tokens and not exposed with default visual styling.
- Chart-heavy screens use `vue-data-ui` unless a module-specific charting need requires a different component.
- Shared components are used for cards, tables, badges, drawers, modals, charts, empty/error/loading states, upload queues, and gates.
- Every module has desktop, tablet, and mobile behavior.
- Every high-risk operation has confirmation.
- Every async task has progress, failure, and retry UI.
- API contracts and backend capability boundaries are documented for each module.
- Automated route/component/accessibility contract tests pass.
- Browser validation through gstack `/browse` passes for required viewports.
