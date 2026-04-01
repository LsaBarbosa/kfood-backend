package com.kfood.merchant.app;

import com.kfood.merchant.domain.LegalDocumentType;
import java.time.Instant;
import java.util.UUID;

public record StoreTermsAcceptanceHistoryItemOutput(
    UUID id,
    UUID acceptedByUserId,
    LegalDocumentType documentType,
    String documentVersion,
    Instant acceptedAt) {}
