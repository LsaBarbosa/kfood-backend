package com.kfood.payment.app.gateway;

import java.time.OffsetDateTime;

public record CreatePixChargeResponse(
    String providerName,
    String providerReference,
    String qrCodePayload,
    OffsetDateTime expiresAt) {}
