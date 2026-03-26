package com.kfood.merchant.api;

import com.kfood.merchant.domain.LegalDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStoreTermsAcceptanceRequest(
    @NotNull LegalDocumentType documentType,
    @NotBlank @Size(max = 40) String documentVersion) {}
