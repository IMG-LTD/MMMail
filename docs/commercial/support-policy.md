# MMMail Commercial Support Policy

This document defines public support wording for MMMail Free and early commercial pilots.

## Public Repository Support

MMMail Free support is handled through public issue triage unless the report contains sensitive security details.

Use:

- `SUPPORT.md` for routing
- `docs/release/v2-support-boundaries.md` for GA / Beta / Preview scope
- `docs/release/v2-feedback-intake.md` for issue evidence requirements
- `SECURITY.md` for live or undisclosed vulnerabilities

## Response Targets

Response targets are best effort and are not SLA commitments.

The current targets are recorded in `MAINTAINERS.md`. They are triage targets for the public repository, not contractual response or resolution times.

## Commercial Pilot Support

Commercial pilot support may be discussed privately with early users, but this repository must not claim:

- contractual response guarantee
- contractual resolution guarantee
- around-the-clock support term
- managed incident response
- uptime credits
- dedicated account management

These terms require a separate commercial agreement and must not be implied by public docs, issue templates, release notes, or UI copy.

## Security Reports

Security-sensitive reports follow `SECURITY.md`.

Do not request exploit details, logs, credentials, or reproduction steps in a public issue. Use the security contact request path only to establish a private handoff.

## Release Blockers

GA regressions remain release blockers for Free users.

Commercial work must not downgrade release-blocking treatment for:

- authentication
- organization isolation
- data access
- public share access
- self-hosted install / upgrade / backup / restore
- documented GA workflows
