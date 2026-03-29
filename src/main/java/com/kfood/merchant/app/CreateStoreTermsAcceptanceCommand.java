package com.kfood.merchant.app;

import com.kfood.merchant.domain.LegalDocumentType;

public record CreateStoreTermsAcceptanceCommand(
    LegalDocumentType documentType, String documentVersion) {}
