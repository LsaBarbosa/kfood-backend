package com.kfood.payment.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentWebhookRequest(
    @NotBlank(message = "externalEventId is required") String externalEventId,
    @NotBlank(message = "eventType is required") String eventType,
    @NotBlank(message = "providerReference is required") String providerReference,
    String paidAt,
    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "amount must be greater than or equal to zero")
        BigDecimal amount) {}
