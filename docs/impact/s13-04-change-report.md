# S13-04 Change Report

## `src/main/java/com/kfood/payment/infra/persistence/PaymentWebhookEvent.java`
- Technical reason:
  The webhook event needed a new explicit terminal status for controlled processing failures without breaking the provider response.
- Why it could not be avoided:
  S13-04 requires a non-destructive, audit-ready outcome when a confirmed event cannot be correlated or applied.
- Risk:
  Low. The change is restricted to webhook operational state.
- Validation:
  Covered by webhook entity, use case, repository integration, Flyway migration, and full `check`.

## `src/main/java/com/kfood/payment/infra/persistence/PaymentRepository.java`
- Technical reason:
  Webhook confirmation needs correlation by `provider_name + provider_reference` with the order eagerly available.
- Why it could not be avoided:
  The existing repository already owned the correlation query, but it did not guarantee the associated order was loaded for snapshot update.
- Risk:
  Low. The added query is specific to webhook confirmation and does not change existing manual flows.
- Validation:
  Covered by repository integration and webhook confirmation tests.
