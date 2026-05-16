# MMMail OpenTelemetry Runtime Tracing

This document describes the v2.2 runtime tracing surface for the public MMMail repository. It is an operator diagnostic guide, not a public SLA and not a contractual commitment.

## Default Boundary

OpenTelemetry is disabled by default:

```env
MMMAIL_OTEL_ENABLED=false
MMMAIL_OTEL_SAMPLING_PROBABILITY=1.0
OTEL_SERVICE_NAME=mmmail-server
OTEL_TRACES_EXPORTER=none
OTEL_EXPORTER_OTLP_ENDPOINT=
```

With the default values, the server must start without an OTLP collector. Do not add fallback success paths for missing collectors; leave exporter failures visible when tracing is explicitly enabled and the endpoint is wrong.

## Enable OTLP Export

For a self-hosted deployment with an OpenTelemetry Collector:

```env
MMMAIL_OTEL_ENABLED=true
MMMAIL_OTEL_SAMPLING_PROBABILITY=1.0
OTEL_SERVICE_NAME=mmmail-server
OTEL_TRACES_EXPORTER=otlp
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318/v1/traces
```

For Helm, set the matching values:

```yaml
commercial:
  otel:
    enabled: true
    serviceName: mmmail-server
    tracesExporter: otlp
    endpoint: http://otel-collector:4318/v1/traces
```

## Span Surface

| Span | Source | Required tags |
|---|---|---|
| `mmmail.http.request` | `RequestTracingFilter` | `component`, `module`, `method`, `route`, `status` |
| `mmmail.db.operation` | commercial JDBC repositories | `component`, `table`, `operation` |
| `mmmail.redis.operation` | public share Redis limiter | `component`, `operation`, `action` |
| `mmmail.billing.webhook` | billing webhook service | `component`, `provider`, `status` |
| `mmmail.license.verify` | license sync service | `component`, `operation` |
| `mmmail.oidc.callback` | OIDC callback service | `component`, `status` |

Errors are recorded on the active observation and then rethrown. The tracing layer must not turn a failing request, webhook, repository operation, Redis call, or license check into a success.

## Log Correlation

`RequestTracingFilter` keeps the existing request id in structured logs through the `traceId` and `requestId` MDC keys. When OpenTelemetry is enabled, request spans and logs can be correlated by request path, status, request id, and collector trace context.

## OIDC Boundary

The main repository now includes the OIDC callback endpoint and the `mmmail.oidc.callback` span wrapper. This covers the in-repository callback tracing surface for BUS-01 and OBS-01: callback success and failure must flow through explicit tracing and error propagation, not a fallback success path.

This document still does not complete the external evidence requirement. BUS-01 and OBS-01 remain partial until a live Keycloak or approved OIDC IdP run produces redacted login, callback, MMMail session, logout, token refresh, and correlated callback trace evidence.

## Verification

Use the targeted backend contract:

```bash
timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22OpenTelemetryContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The contract checks:

- OpenTelemetry dependencies and environment config
- disabled-by-default env templates
- Helm config exposure
- HTTP, DB, Redis, billing webhook, and license verification span wiring
- OIDC callback span wiring through `BackendV22OidcSsoContractTest`
- `RuntimeTraceService` error propagation
- README, local gate, CI, and spec discovery
