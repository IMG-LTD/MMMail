# Changelog

## v2.0.4
- Added Bash and PowerShell one-click installers for minimal and standard Docker Compose deployment paths.
- Restructured first-time install documentation around one-click, Docker manual, bare-metal manual, and local development paths.
- Added browser-local first-login onboarding with four quick-start steps and a Settings reopen entry.

## v2.0.3
- Preserved the mixed legacy repository state on `archive/v2-only-pre-cleanup-20260423` before simplifying `main`.
- Replaced the last `frontend/` runtime, compose, CI, and validation entrypoints with `frontend-v2`.
- Refreshed install, upgrade, runbook, support-boundary, and release docs for the v2-only mainline.

## v2.0.2
- Closed the remaining frontend route/runtime and backend billing-readiness hardening gaps within the shipped `v2.0.0` boundary.

## v2.0.1
- Promoted frontend-v2 and backend capability hardening regressions into the release gate without changing the shipped top-level product boundary.

## v2.0.0
- Replaced the v1 execution baseline with the MMMail v2 route, contract, and milestone architecture.
- Added extraction-ready backend modules and frontend-v2 shared contracts.
- Promoted release validation so `frontend-v2` now participates in local and CI release gates.
