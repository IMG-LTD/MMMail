# Image Digest Evidence Template

This template defines the external evidence required before DEP-02 can move from partial done to done. It is not a substitute for a real tag-triggered image publishing workflow run.

## Evidence Package

Create one redacted evidence package per release tag:

- Release tag:
- Git commit SHA:
- GitHub workflow name:
- GitHub workflow run URL:
- Workflow event:
- Workflow conclusion:
- Workflow started at:
- Workflow finished at:
- Registry:
- Backend image name:
- Backend immutable digest:
- Frontend image name:
- Frontend immutable digest:
- Operator:

## Required Evidence

| Item | Required evidence |
|---|---|
| Tag trigger | Workflow run shows a `push` event for the release tag, not only `workflow_dispatch` |
| Backend image | `mmmail-backend` image pushed to GHCR with `linux/amd64` and `linux/arm64` support |
| Frontend image | `mmmail-frontend-admin` image pushed to GHCR with `linux/amd64` and `linux/arm64` support |
| Immutable digests | Backend and frontend image digests recorded as `sha256:*` |
| Release notes | Release notes include both image names, tags, and immutable digests |

## Non-Evidence

These do not satisfy DEP-02:

- Local Docker builds.
- `workflow_dispatch` runs without tag-published image digests.
- A workflow file existing only in the local working tree.
- Mutable tags without immutable digests.
- GHCR package listings without a matching workflow run URL and commit SHA.
