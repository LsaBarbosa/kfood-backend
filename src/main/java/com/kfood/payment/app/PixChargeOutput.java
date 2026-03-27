package com.kfood.payment.app;

import java.time.OffsetDateTime;

public record PixChargeOutput(
    String providerName, String providerReference, String qrCodePayload, OffsetDateTime expiresAt) {}
