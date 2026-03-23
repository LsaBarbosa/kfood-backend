package com.kfood.eventing.infra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.eventing.rabbit")
public record RabbitTopologyProperties(
    @NotBlank String exchange,
    @DefaultValue("true") boolean startupVerify,
    @NotNull @Valid Route orderCreated,
    @NotNull @Valid Route orderStatusChanged,
    @NotNull @Valid Route paymentConfirmed) {

  public record Route(@NotBlank String queue, @NotBlank String routingKey) {}
}
