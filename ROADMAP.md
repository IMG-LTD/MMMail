# MMMail Roadmap

This roadmap is intentionally conservative. It describes public direction, not a promise of delivery dates or commercial availability.

## Current Baseline

- Current shipped tag: `v2.1.2-shipping-clean`
- Public self-hosted line: MMMail Free
- Product frontend: `frontend-admin`
- Legacy reference frontend: `frontend-v2`

## v2.2 Focus

- Open source trust layer: governance, DCO, NOTICE, maintainer ownership, security disclosure path.
- Frontend convergence: `frontend-admin` becomes the only product frontend; `frontend-v2` exits product release paths.
- Commercial-ready contracts: Free / Pro / Business edition model, license verification, billing provider contract, webhook schema.
- Business access baseline: OIDC, audit export, DSR/data inventory.
- Deployment and observability: Helm, image publishing, OpenTelemetry, SLI/SLO docs, expanded release gates.

## v2.3 Direction

- Remove or externally archive `frontend-v2` after contract migration.
- SCIM 2.0 and SAML 2.0 evaluation.
- Custom RBAC and status page.
- Customer-success runbooks and SDK generation.

## v2.4+ Direction

- BYOK.
- Multi-region and data residency.
- SOC2 / ISO 27001 documentation framework.
- Marketplace, CLI, and mobile shell exploration.

## Non-Commitments

- No 24/7 SLA is promised by the public repository.
- No SOC2, ISO, multi-region, or hosted operations guarantee exists until a dedicated commercial program publishes those terms.
- Real payment processing, merchant credentials, refunds, invoices, and license signing private keys stay outside the public repository.
