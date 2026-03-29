package com.kfood.merchant.app;

public final class StoreTermsAcceptanceMapper {

  private StoreTermsAcceptanceMapper() {}

  public static StoreTermsAcceptanceOutput toOutput(MerchantViews.StoreTermsAcceptanceView acceptance) {
    return new StoreTermsAcceptanceOutput(
        acceptance.id(),
        acceptance.documentType(),
        acceptance.documentVersion(),
        acceptance.acceptedAt());
  }

  public static StoreTermsAcceptanceHistoryItemOutput toHistoryItemOutput(
      MerchantViews.StoreTermsAcceptanceView acceptance) {
    return new StoreTermsAcceptanceHistoryItemOutput(
        acceptance.id(),
        acceptance.acceptedByUserId(),
        acceptance.documentType(),
        acceptance.documentVersion(),
        acceptance.acceptedAt());
  }
}
