package com.kfood.eventing.app;

import java.util.UUID;

public record EventEnvelope<T>(
    UUID eventId,
    String eventType,
    int version,
    String occurredAt,
    String tenantId,
    String correlationId,
    T payload) {}
