# Gateway Compatibility Matrix

| Legacy Route | Canonical Route | Rewrite Phase | Notes |
|---|---|---|---|
| `/mail/:id` | `/conversations/:id` | M1 -> M2 | preserve query and hash |
| `/conversations` | `/inbox` | M1 -> M2 | preserve safe query only |
| `/folders/:folderId` | `/folders/:id` | M1 -> M2 | parameter rename only |
| `/labels` | `/inbox` | M1 -> M2 | default label landing |
| `/public/drive/shares/:token` | `/share/drive/:token` | M1 -> M2 | public compatibility alias |
| `/settings/system-health` | `/settings?panel=system-health` | M1 -> M2 | governance panel merge |
| `/pass-monitor` | `/pass/monitor` | already present; keep in matrix | preserve query |
