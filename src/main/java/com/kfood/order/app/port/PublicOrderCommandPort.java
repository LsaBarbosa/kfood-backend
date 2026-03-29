package com.kfood.order.app.port;

import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.order.app.CreatePublicOrderCommand;
import com.kfood.order.app.CreatePublicOrderOutput;
import java.util.Optional;
import java.util.UUID;

public interface PublicOrderCommandPort {

  Optional<StoreReference> findStoreBySlug(String slug);

  CreatePublicOrderOutput createOrder(
      UUID storeId, CreatePublicOrderCommand command, CheckoutQuoteSnapshot quote);

  record StoreReference(UUID id) {}
}
