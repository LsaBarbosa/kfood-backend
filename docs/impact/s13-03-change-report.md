# S13-03 Change Report

## `src/main/java/com/kfood/payment/app/RegisterPaymentWebhookUseCase.java`
- Technical reason:
  The use case needed to become idempotent without violating the architecture rule that forbids `payment.app` from depending directly on `payment.infra`.
- Why it could not be avoided:
  The initial implementation used `PaymentWebhookEventRepository` directly in the application layer. S13-03 requires race-safe deduplication logic in the use case itself, so the dependency had to be inverted through a dedicated persistence port.
- Risk:
  Low. The behavioral scope remains limited to webhook event receipt and idempotent registration.
- Validation:
  Covered by `RegisterPaymentWebhookUseCaseTest`, `PaymentWebhookControllerWebMvcTest`, `PaymentWebhookEventPersistenceAdapterTest`, `ArchitectureTest`, and `BaseDependenciesTest`.

## `src/main/java/com/kfood/payment/infra/persistence/PaymentWebhookEvent.java`
- Technical reason:
  The persistence entity now implements an application-facing record contract so the use case can remain isolated from infrastructure classes.
- Why it could not be avoided:
  Without a small contract implemented by the entity, the application layer would still need to depend on an infrastructure type or require unnecessary mapping code for this feature.
- Risk:
  Low. No table, field, or business rule was changed; only the implemented interface set changed.
- Validation:
  Covered by existing entity tests plus `PaymentWebhookEventPersistenceAdapterTest`, `ArchitectureTest`, and the feature tests.
