# MMMail v2 Orchestration Log

| Timestamp | Actor | Scope | Decision | Evidence |
|---|---|---|---|---|
| 2026-04-20T00:00:00Z | main-agent | M0 | Confirmed existing release branch, then initialized worktree root and progress ledger. | `git branch --list release/2.0.0` |

## Escalation Rules
- Contract schema breaks pause at G-slice and require user approval.
- Destructive migrations pause at G-slice and require user approval.
- New runtime dependencies pause before merge.
- Legacy file deletion before M6 is forbidden.
