package com.kfood.merchant.app;

import com.kfood.merchant.domain.LegalDocumentType;
import java.time.Instant;

public record CreateStoreTermsAcceptanceCommand(
    LegalDocumentType documentType, String documentVersion, Instant acceptedAt) {

  public CreateStoreTermsAcceptanceCommand(LegalDocumentType documentType, String documentVersion) {
    this(documentType, documentVersion, null);
  }
}
