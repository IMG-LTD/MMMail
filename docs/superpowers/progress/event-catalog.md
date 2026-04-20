# Initial Event Catalog

| Event | Domain | Trigger |
|---|---|---|
| `identity.session.created.v1` | identity | login or register |
| `identity.session.revoked.v1` | identity | logout or admin revoke |
| `platform.outbox.enqueued.v1` | platform | any async action persisted |
| `platform.audit.recorded.v1` | platform | security or governance action |
| `mail.secure-link.opened.v1` | mail | public mail share viewed |
| `drive.share.downloaded.v1` | drive | drive public share download |
| `pass.secure-link.unlocked.v1` | pass | pass public share unlock |
| `workspace.comment.created.v1` | workspace | docs or sheets collaboration comment |
| `billing.plan.changed.v1` | billing | subscription or plan change |
| `labs.module.reviewed.v1` | labs | preview module decision logged |
