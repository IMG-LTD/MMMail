# Private Billing Evidence Template

This template defines the external evidence required before the independent billing repository and real payment/license-signing items can move from external to done. It is not a substitute for a real private billing repository, payment provider sandbox/live run, or private license signing key.

## Evidence Package

Create one redacted evidence package per billing gateway commit:

- Billing repository URL:
- Billing repository commit SHA:
- Public MMMail repository commit SHA:
- Payment provider:
- Provider environment:
- Webhook endpoint URL:
- Customer portal URL or boundary record:
- License signing key location:
- Run started at:
- Run finished at:
- Operator:

Do not include merchant credentials, provider secrets, customer personal data, private signing keys, or raw invoices.

## Required Evidence

| Item | Required evidence |
|---|---|
| Provider adapter | Redacted proof that the billing gateway uses the real provider adapter, not `none` or mock success |
| Webhook delivery | Provider webhook delivery record plus corresponding MMMail webhook event ID |
| Customer portal | Customer portal boundary or redirect evidence with sensitive customer data redacted |
| Invoice / refund | Redacted invoice and refund lifecycle evidence tied to provider event IDs |
| License signing | Redacted license signing run showing private key use outside the public repository and public claims accepted by MMMail |
| Idempotency | Replayed webhook evidence showing stable idempotency behavior |

## Non-Evidence

These do not satisfy the billing evidence requirement:

- Public repository webhook unit tests.
- `none` provider events.
- Mock paid subscription state.
- License claims generated without the private signing key.
- Screenshots without provider event IDs, timestamps, commit SHAs, and redacted license identifiers.
- A private repository name without accessible commit or run evidence.
