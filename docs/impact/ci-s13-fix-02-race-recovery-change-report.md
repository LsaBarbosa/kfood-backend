# CI S13 Fix 02 Change Report

- File changed: `src/main/java/com/kfood/payment/infra/persistence/PaymentWebhookEventPersistenceAdapter.java`
  - Technical reason: in the real JPA/PostgreSQL race path, the unique constraint violation can surface as `JpaSystemException` wrapping `GenericJDBCException` and `PSQLException`, instead of arriving as `DataIntegrityViolationException`.
  - Why the change was necessary: the use case already recovers idempotently from `DataIntegrityViolationException`, but the adapter was leaking a provider-specific exception shape that bypassed that recovery path in CI.
  - Risk: low. The adapter now translates only the known PostgreSQL unique-violation case for `uk_payment_webhook_event_provider_external_event` and still rethrows unrelated persistence failures unchanged.
  - Validation executed: `RegisterPaymentWebhookUseCaseIntegrationTest`, `RegisterPaymentWebhookUseCaseTest`, `PaymentWebhookEventPersistenceAdapterTest`, and `check`.

- File changed: `src/test/java/com/kfood/payment/infra/persistence/PaymentWebhookEventPersistenceAdapterTest.java`
  - Technical reason: add explicit coverage for the real exception shape observed in CI and prove unrelated exceptions are not swallowed.
  - Risk: none in production.
  - Validation executed: same test suite above.
