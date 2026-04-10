# Security Policy

## Scope
- `Community Edition v1.6.1` is the current public baseline.
- Security fixes that affect authentication, organization isolation, data access, storage, self-hosted deployment, and documented GA capabilities are treated as release blockers.

## Reporting a Vulnerability
- Do **not** open a public GitHub issue for a live security vulnerability.
- If GitHub private vulnerability reporting is enabled, use it first.
- Otherwise contact the repository maintainers through a private channel already established outside the repository.
- Public issues may be used only after the vulnerability is fixed or explicitly approved for disclosure.
- 详细威胁边界见 `docs/security/threat-model.md`。

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

## Community Edition v1.6.1 Baseline
- `scripts/security-secret-scan.sh`：扫描工作树文件中的 secrets 回归
- `scripts/validate-security.sh`：执行 secrets scan + 安全回归
- `scripts/security-backend-dependency-scan.sh`：执行后端 OWASP Dependency-Check
- CI 必须执行：
  - `MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN=true bash scripts/validate-security.sh`
- 自托管建议：
  - 生产环境开启 TLS
  - 设置 `MMMAIL_AUTH_COOKIE_SECURE=true`
  - 轮转 `.env` 中全部 secrets
  - 限制 `actuator` 与管理接口暴露范围

## Current Community v1.6.1 Status
- The repository baseline includes secret scanning, backend dependency scanning, login/client-error rate limiting, and documented deployment hardening.
- Remaining release blockers are tracked in `docs/release/community-v1-gate.md`.
