# Table Ownership Matrix

| Prefix / Table Family | Current Owner | Target Module | Tenant Rule |
|---|---|---|---|
| `user_*` | server monolith | `mmmail-identity` | global user + tenant membership bridge |
| `org_*` | server monolith | `mmmail-org-governance` | tenant-scoped |
| `mail_*` | server monolith | `mmmail-mail` | tenant-scoped |
| `drive_*` | server monolith | `mmmail-drive` | tenant-scoped |
| `pass_*` | server monolith | `mmmail-pass` | tenant-scoped |
| `docs_*` | server monolith | `mmmail-workspace` | tenant-scoped |
| `sheet_*` | server monolith | `mmmail-workspace` | tenant-scoped |
| `suite_*` | server monolith | `mmmail-billing` | tenant-scoped |
| `audit_*` | server monolith | `mmmail-platform` | tenant-scoped |
