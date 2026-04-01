package com.kfood.checkout.app;

import java.util.Optional;
import java.util.UUID;

public interface CheckoutQuoteSnapshotGateway {

  CheckoutQuoteSnapshot save(CheckoutQuoteSnapshot snapshot);

  Optional<CheckoutQuoteSnapshot> findValidByStoreIdAndQuoteId(UUID storeId, UUID quoteId);
}
