package com.kfood.eventing.infra.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.eventing.outbox")
public record EventingOutboxProperties(
    @Min(1) @DefaultValue("50") int batchSize, @Min(1) @DefaultValue("5000") long retryInterval) {}
