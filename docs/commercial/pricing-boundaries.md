# MMMail Commercial Pricing Boundaries

This document defines what the public repository may say about Free, Pro, Business, and Hosted packaging before a real commercial program is launched.

## Current Position

- MMMail Free is the public self-hosted baseline.
- v2.1.2 GA capabilities remain Free for self-hosted users.
- Pro and Business are v2.2 commercial-ready packaging directions, not a live checkout promise.
- Hosted operations are not generally available from this repository.

## Pricing Statement

No public price is committed in this repository.

Until the separate billing and license-signing program is complete, public copy may use only:

- pilot pricing
- contact-based pricing
- commercial-ready adapter
- planned Pro / Business boundaries

Public copy must not publish fixed prices, refund rules, invoice promises, tax handling, or around-the-clock support guarantees unless those terms are backed by an approved commercial program.

## Payment And License Boundary

Real payment processing is not live in the public repository.

The public repository may contain:

- edition and entitlement checks
- license verification with a public key
- billing provider contracts
- webhook signature contracts
- self-hosted configuration docs

The public repository must not contain:

- merchant credentials
- payment provider private keys
- live checkout credentials
- refund automation
- invoice issuing logic
- license signing private keys

Payment provider adapters, customer portals, invoices, refunds, and license signing private keys stay outside this public repository.

## Free Commitment

Free self-hosted usage must not be weakened to create paid pressure.

The following remain Free in the public baseline:

- v2.1.2 GA capabilities
- Mail / Calendar / Drive / Suite Shell / Settings / Auth baseline
- documented self-hosted install, upgrade, backup, restore, and runbook paths
- public issue triage for release-blocking regressions
- security disclosure intake through `SECURITY.md`

Pro / Business packaging may cover v2.2-and-later additions such as team governance enhancements, Business access controls, audit export, and OIDC SSO after their implementation and gates exist.

## Forbidden Claims

Do not claim:

- payments are live
- checkout is available
- invoices or refunds are available
- hosted service is generally available
- an uptime guarantee exists
- an around-the-clock contractual support term exists
- SOC2 / ISO certification exists
- Pro / Business is required for v2.1.2 GA capabilities

## Allowed Public Language

Allowed:

- "Commercial-ready adapter"
- "Pilot pricing, contact the maintainer"
- "Pro / Business boundaries are planned in v2.2"
- "Real payment processing and license signing are outside this public repository"

Not allowed:

- "Buy now"
- "Live checkout is available"
- "Uptime guarantee"
- "Around-the-clock contractual support"
- "Enterprise certified"
