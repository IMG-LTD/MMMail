# Legacy Exit Report

## Redirected Legacy Routes
- `/mail/:id` -> `/conversations/:id`
- `/conversations` -> `/inbox`
- `/labels` -> `/inbox`
- `/settings/system-health` -> `/settings?panel=system-health`
- `/pass-monitor` -> `/pass/monitor`
- `/public/drive/shares/:token` -> `/share/drive/:token`

## Same-Shape Compatibility
- `/folders/:folderId` -> `/folders/:id` (same Vue Router path shape, tracked as a compatibility contract rather than a standalone redirect)

## Preview Redirects
- `/authenticator` -> `/labs/authenticator`
- `/simplelogin` -> `/labs/simplelogin`
- `/standard-notes` -> `/labs/standard-notes`
- `/vpn` -> `/labs/vpn`
- `/meet` -> `/labs/meet`
- `/wallet` -> `/labs/wallet`
- `/lumo` -> `/labs/lumo`

## Explicit Non-Actions
- No legacy file deletion yet.
- No Meet guest route promotion.
- No new legacy business logic added.
