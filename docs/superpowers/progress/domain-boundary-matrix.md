# Domain Boundary Matrix

| Domain | Target Module | Owns | May Reference | Must Not Own |
|---|---|---|---|---|
| Foundation | `mmmail-foundation` | tenant context, shared headers, base contracts | none | product-specific controllers |
| Identity | `mmmail-identity` | auth, sessions, recovery, trusted device | Foundation | mail, drive, pass business tables |
| Org Governance | `mmmail-org-governance` | org membership, scope, domains, policy | Foundation, Identity | billing ledger, mail runtime |
| Platform | `mmmail-platform` | outbox, jobs, audit, notification, AI, MCP | Foundation, Identity | mailbox contents |
| Mail | `mmmail-mail` | threads, drafts, delivery, mail public share | Foundation, Identity, Platform | drive binary storage |
| Drive | `mmmail-drive` | files, shares, versions, preview metadata | Foundation, Identity, Platform | pass vault items |
| Pass | `mmmail-pass` | vaults, aliases, secure links, policy views | Foundation, Identity, Platform | docs collaboration state |
| Workspace | `mmmail-workspace` | calendar, docs, sheets, collaboration | Foundation, Identity, Platform | billing subscription data |
| Billing | `mmmail-billing` | plans, subscription state, invoices | Foundation, Identity, Org Governance | mailbox or vault data |
| Labs | `mmmail-labs` | preview-only adapters and lifecycle flags | Foundation, Identity, Platform | GA default SDK contracts |
