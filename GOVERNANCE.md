# MMMail Governance

## Model

MMMail uses a BDFL governance model. The BDFL owns final product, architecture, licensing, and release decisions after reviewing maintainer and contributor input.

## Roles

| Role | Responsibility |
|---|---|
| BDFL | Final decision maker for roadmap, release scope, licensing, and disputes |
| Release owner | Runs release gates, validates release notes, and blocks unsafe releases |
| Module owner | Reviews changes in an owned area and keeps module docs current |
| Security owner | Coordinates private vulnerability intake, triage, and disclosure timing |
| Contributor | Opens focused issues or PRs with evidence and validation output |

Current named owners are listed in `MAINTAINERS.md`.

## Decision Records

Decisions that affect public behavior, support boundaries, deployment, licensing, security, or commercial packaging must be recorded in one of:

- `docs/release/*`
- `docs/open-source/*`
- `docs/architecture/*`
- a versioned spec under `docs/`

Implementation-only changes can be documented in the PR body when they do not alter public contracts.

## Release Policy

- GA regressions are release blockers.
- Beta and Preview work must not break GA gates.
- Release gate skips are allowed only for local diagnosis. CI and release candidates must use the full gate.
- A release may not claim commercial, payment, hosted, SLA, or enterprise readiness unless the corresponding docs, tests, and gates exist.

## Dispute Resolution

1. Keep the discussion in the issue or PR and state the technical disagreement clearly.
2. Reference code, tests, docs, security impact, or user impact.
3. Ask the module owner for a recommendation.
4. The BDFL records the final decision when consensus is not reached.

## Succession

If the BDFL is unavailable for a release-blocking security issue, the release owner may freeze releases and coordinate a private fix. Longer-term ownership transfer must be recorded in `MAINTAINERS.md` and `GOVERNANCE.md`.
