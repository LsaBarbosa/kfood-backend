# S13-05 Change Report

## `src/main/java/com/kfood/shared/config/AppProperties.java`
- Technical reason:
  The webhook token must come from external configuration, not from hardcoded source code.
- Why it could not be avoided:
  The project already centralizes application properties in `AppProperties`, so the smallest coherent change was to extend that structure.
- Risk:
  Low. The new property branch is additive and does not change existing security or payment settings.
- Validation:
  Covered by configuration binding and webhook authentication tests, plus full `check`.

## `src/main/java/com/kfood/payment/api/PaymentWebhookController.java`
- Technical reason:
  Webhook authentication must happen before payload persistence and any business effect.
- Why it could not be avoided:
  The controller is the earliest stable point in the request flow to reject invalid requests without touching the existing webhook registration and confirmation logic.
- Risk:
  Low. The endpoint contract remains the same on success, and invalid requests are now rejected with the project-standard unauthorized error.
- Validation:
  Covered by WebMvc tests for valid, missing, blank, and invalid tokens, plus full `check`.
