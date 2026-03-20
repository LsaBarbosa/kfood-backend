package com.kfood.merchant.api;

import com.kfood.merchant.domain.LegalDocumentType;
import java.time.Instant;
import java.util.UUID;

public record StoreTermsAcceptanceResponse(
    UUID id, LegalDocumentType documentType, String documentVersion, Instant acceptedAt) {}
