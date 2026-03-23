package com.kfood.eventing.app;

import java.util.UUID;

public record ConsumedEventMetadata(UUID eventId, String eventType, String aggregateId) {}
