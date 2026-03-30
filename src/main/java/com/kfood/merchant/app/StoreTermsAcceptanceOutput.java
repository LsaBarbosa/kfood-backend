package com.kfood.merchant.app;

import com.kfood.merchant.domain.LegalDocumentType;
import java.time.Instant;
import java.util.UUID;

public record StoreTermsAcceptanceOutput(
    UUID id, LegalDocumentType documentType, String documentVersion, Instant acceptedAt) {}
