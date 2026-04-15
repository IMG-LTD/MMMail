# Security Policy

## Scope
- `Community Edition v1.6.1` is the current public baseline.
- Security fixes that affect authentication, organization isolation, data access, storage, self-hosted deployment, or documented `GA` capabilities are treated as release blockers.

## Reporting a Vulnerability
- Do **not** open a public GitHub issue for a live security vulnerability.
- If GitHub private vulnerability reporting is enabled, use it first.
- Otherwise contact the repository maintainers through a private channel already established outside the repository.
- Public issues may be used only after the vulnerability is fixed or explicitly approved for disclosure.
- Threat boundaries are documented in `docs/security/threat-model.md`.

## Immediate Rotation Required
- The repository previously contained real-looking local infrastructure credentials in example files.
- Those values must be treated as exposed and rotated outside the repository before any shared environment is trusted.
- Rotate at minimum:
  - MySQL business account password
  - MySQL root password if it was reused anywhere
  - Redis password
  - Nacos service or console password if reused
  - JWT signing secret used by any environment
  - Kafka bootstrap target if it points to a non-local shared cluster

## Baseline Rules
- Never commit live secrets, passwords, API keys, JWT secrets, or private infrastructure endpoints into the repository.
- Keep example files sanitized and use `replace-with-*` placeholders only.
- Production secrets must come from environment variables, secret managers, or deployment-time injection.
- Security-sensitive changes must include:
  - explicit error handling
  - audit visibility where applicable
  - updated validation or regression coverage

## Security Validation
- `scripts/security-secret-scan.sh` scans the working tree for secret regressions.
- `scripts/validate-security.sh` runs secret scanning plus security regressions.
- `scripts/security-backend-dependency-scan.sh` runs backend OWASP Dependency-Check.
- CI must run:
  - `MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN=true bash scripts/validate-security.sh`

## Self-Hosted Hardening Basics
- Enable TLS in production.
- Set `MMMAIL_AUTH_COOKIE_SECURE=true`.
- Rotate all secrets in `.env` before sharing an environment.
- Limit `actuator` and management endpoint exposure to trusted networks.
- Keep deployment and backup procedures aligned with `docs/ops/install.md`, `docs/ops/upgrade.md`, `docs/ops/backup-restore.md`, and `docs/ops/runbook.md`.
