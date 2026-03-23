package com.kfood.eventing.app;

import java.util.UUID;

public record OutboxCreatedEvent(UUID outboxEventId) {}
