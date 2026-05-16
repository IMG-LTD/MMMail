# MMMail Helm Deployment

This chart is the v2.2 self-hosted Kubernetes entry for MMMail. It deploys only:

- `backend`
- `frontend-admin`

It does not deploy `frontend-v2`.

## Prerequisites

- Kubernetes 1.27+
- Helm 3
- External MySQL 8.4 compatible database
- External Redis 7 compatible instance
- Published `mmmail-backend` and `mmmail-frontend-admin` images

## Runtime Secret

The chart expects sensitive values to come from a Kubernetes Secret. `values.yaml` does not contain secret defaults.

```bash
kubectl create secret generic mmmail-runtime \
  --from-literal=SPRING_DATASOURCE_PASSWORD='replace-with-db-password' \
  --from-literal=SPRING_REDIS_PASSWORD='replace-with-redis-password' \
  --from-literal=MMMAIL_JWT_SECRET='replace-with-32-plus-char-random-secret' \
  --from-literal=MMMAIL_BILLING_WEBHOOK_SECRET='replace-with-32-plus-char-random-secret' \
  --from-literal=MMMAIL_LICENSE_PUBLIC_KEY='replace-with-ed25519-x509-public-key'
```

OIDC client secrets use the same Secret when Business SSO is enabled:

```bash
kubectl create secret generic mmmail-runtime \
  --from-literal=MMMAIL_OIDC_CLIENT_SECRET='replace-with-oidc-client-secret' \
  --dry-run=client -o yaml | kubectl apply -f -
```

## Minimal Values

```yaml
secrets:
  existingSecret: mmmail-runtime

externalDatabase:
  host: mysql.example.internal
  port: 3306
  name: mmmail
  username: mmmail_app

externalRedis:
  host: redis.example.internal
  port: 6379
  database: 0

global:
  publicBaseUrl: https://mail.example.com
  corsAllowedOrigins: https://mail.example.com

frontend:
  apiBaseUrl: https://api.example.com

commercial:
  billing:
    provider: none
  oidc:
    enabled: false
  auditExport:
    enabled: false
  otel:
    enabled: false
```

## Validate And Render

```bash
helm lint helm/mmmail
helm template mmmail helm/mmmail
bash scripts/validate-helm-chart.sh
```

## Install

```bash
helm upgrade --install mmmail helm/mmmail \
  --namespace mmmail \
  --create-namespace \
  -f values.self-hosted.yaml
```

## Commercial And Enterprise Knobs

The chart exposes values for:

- license public key injection through `MMMAIL_LICENSE_PUBLIC_KEY`
- billing provider and webhook secret injection
- OIDC issuer, client id, and client secret injection
- audit export enablement and retention days
- OpenTelemetry service name and OTLP endpoint

These values only configure the public repository runtime surface. Real payment provider adapters, license signing private keys, and merchant credentials stay outside the public repository.
