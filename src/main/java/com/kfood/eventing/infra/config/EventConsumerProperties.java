package com.kfood.eventing.infra.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.eventing.rabbit.consumer")
public record EventConsumerProperties(
    @Min(1) @DefaultValue("3") int maxAttempts,
    @Min(100) @DefaultValue("1000") long initialIntervalMs,
    @DecimalMin("1.0") @DefaultValue("2.0") double multiplier,
    @Min(1000) @DefaultValue("10000") long maxIntervalMs,
    @NotBlank @DefaultValue("kfood.events.dlx") String dlxExchange,
    @NotBlank @DefaultValue("dlq.") String dlqRoutingPrefix) {}
