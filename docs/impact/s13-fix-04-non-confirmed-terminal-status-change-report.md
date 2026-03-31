# S13 Fix 04 Change Report

- File altered: `src/main/java/com/kfood/payment/app/RegisterPaymentWebhookUseCase.java`
  - Technical reason:
    - Valid webhook events with `eventType` different from `PAYMENT_CONFIRMED` were being accepted and left indefinitely in `RECEIVED`.
    - The use case now marks those accepted non-actionable events as `PROCESSED` immediately after persistence.
  - Risk:
    - Low. No HTTP contract or financial behavior was changed; only the operational terminal status of non-confirmed events was corrected.
  - Validation executed:
    - Unit tests for non-confirmed event handling.
    - PostgreSQL/Testcontainers integration tests covering replay and race behavior for non-confirmed events.

- File altered: `src/main/java/com/kfood/payment/infra/persistence/PaymentWebhookEventPersistenceAdapter.java`
- File altered: `src/main/java/com/kfood/payment/app/port/PaymentWebhookEventPersistencePort.java`
  - Technical reason:
    - `markProcessed(...)` now supports terminal processing without correlated payment, which is required for accepted non-confirmed events.
  - Risk:
    - Low. The change is additive for the existing confirmed-payment path and preserves payment attachment when `paymentId` is present.
  - Validation executed:
    - Adapter unit tests.
    - Full `./gradlew --no-daemon check`.
