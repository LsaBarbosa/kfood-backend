# S13 Fix 02 Change Report

- File changed: `src/main/java/com/kfood/payment/infra/persistence/PaymentWebhookEventPersistenceAdapter.java`
- Technical reason:
  - The adapter used `save(...)` without an explicit flush, so the unique-constraint violation on `(provider_name, external_event_id)` could surface only at transaction commit time instead of inside `RegisterPaymentWebhookUseCase`.
  - That made the application-level `DataIntegrityViolationException` recovery path less reliable with real JPA/PostgreSQL behavior than in the existing mock-based tests.
- Risk:
  - Low. The change only forces the insert to flush at the adapter boundary where the use case already expects and handles uniqueness failures idempotently.
- Validation executed:
  - PostgreSQL/Testcontainers integration test proving the adapter now raises the unique-constraint violation during `saveReceivedEvent(...)`.
  - PostgreSQL/Testcontainers integration test proving `RegisterPaymentWebhookUseCase` recovers idempotently when a concurrent insert wins between lookup and insert.
  - Targeted webhook/persistence Gradle test suite.
  - Full `./gradlew --no-daemon check`.
