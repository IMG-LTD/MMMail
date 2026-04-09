# MMMail Community v1.6.1 Identity Readiness

## Scope

This document is a readiness guide for self-hosted operators who expect future
enterprise identity requirements.

It is not a claim that Community v1.6.1 already ships:

- SSO login
- SCIM provisioning
- LDAP sync
- automated lifecycle management

## Why this exists

Enterprise evaluators will ask for identity controls early. If the product cannot
answer honestly, teams either over-promise or block adoption entirely.

The goal of readiness is narrower:

1. make the current gap explicit
2. prepare deployment decisions in advance
3. reduce rework when identity automation eventually lands

## Current boundary

Community v1.6.1 still uses local MMMail accounts for sign-in and lifecycle.

Readiness means planning around that boundary, not hiding it.

## Operator checklist

### 1. Identity source inventory

- Which IdP owns workforce identity today
- Which domains are authoritative
- Which teams need strict joiner / mover / leaver controls

### 2. Fallback admin model

- Keep at least one local break-glass administrator
- Document password reset and recovery ownership
- Avoid depending on future SSO before it exists

### 3. Directory hygiene

- Normalize display names, primary emails, and domain ownership
- Decide how aliases, shared mailboxes, and service accounts should be represented
- Record which group structures map to MMMail organizations or roles

### 4. Provisioning expectations

- Decide whether future provisioning must be push-based, pull-based, or manual
- List required lifecycle events: create, suspend, rename, delete, transfer
- Capture audit requirements before procurement or rollout reviews

### 5. Security and rollout constraints

- Define session revocation expectations
- Define MFA / device trust expectations outside MMMail
- Decide what must remain blocked until identity automation exists

## Recommended product positioning

Use this wording with evaluators:

> MMMail Community v1.6.1 provides identity readiness guidance, not enterprise identity automation.
> Teams can prepare directory ownership, fallback admins, and rollout policy now, while keeping login on local MMMail accounts.

## Related docs

- `docs/ops/install.md`
- `docs/ops/runbook.md`
- `docs/ops/team-enablement.md`
- `docs/open-source/module-maturity-matrix.md`

