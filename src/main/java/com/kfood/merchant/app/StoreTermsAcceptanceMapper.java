package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.StoreTermsAcceptance;

public final class StoreTermsAcceptanceMapper {

  private StoreTermsAcceptanceMapper() {}

  public static StoreTermsAcceptanceOutput toOutput(StoreTermsAcceptance acceptance) {
    return new StoreTermsAcceptanceOutput(
        acceptance.getId(),
        acceptance.getDocumentType(),
        acceptance.getDocumentVersion(),
        acceptance.getAcceptedAt());
  }

  public static StoreTermsAcceptanceHistoryItemOutput toHistoryItemOutput(
      StoreTermsAcceptance acceptance) {
    return new StoreTermsAcceptanceHistoryItemOutput(
        acceptance.getId(),
        acceptance.getAcceptedByUserId(),
        acceptance.getDocumentType(),
        acceptance.getDocumentVersion(),
        acceptance.getAcceptedAt());
  }
}
