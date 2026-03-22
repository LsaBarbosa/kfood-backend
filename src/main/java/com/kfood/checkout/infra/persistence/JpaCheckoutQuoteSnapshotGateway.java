package com.kfood.checkout.infra.persistence;

import com.kfood.checkout.app.CheckoutQuoteItemSnapshot;
import com.kfood.checkout.app.CheckoutQuoteOptionSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshotGateway;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaCheckoutQuoteSnapshotGateway implements CheckoutQuoteSnapshotGateway {

  private final CheckoutQuoteRepository checkoutQuoteRepository;

  public JpaCheckoutQuoteSnapshotGateway(CheckoutQuoteRepository checkoutQuoteRepository) {
    this.checkoutQuoteRepository = checkoutQuoteRepository;
  }

  @Override
  public CheckoutQuoteSnapshot save(CheckoutQuoteSnapshot snapshot) {
    var entity =
        new CheckoutQuote(
            snapshot.quoteId(),
            snapshot.storeId(),
            snapshot.customerId(),
            snapshot.fulfillmentType(),
            snapshot.addressId(),
            snapshot.subtotalAmount(),
            snapshot.deliveryFeeAmount(),
            snapshot.totalAmount(),
            snapshot.expiresAt());
    for (var item : snapshot.items()) {
      var itemEntity =
          new CheckoutQuoteItem(
              UUID.randomUUID(),
              item.productId(),
              item.productNameSnapshot(),
              item.unitPriceSnapshot(),
              item.quantity(),
              item.notes());
      for (var option : item.options()) {
        itemEntity.addOption(
            new CheckoutQuoteItemOption(
                UUID.randomUUID(),
                option.optionNameSnapshot(),
                option.extraPriceSnapshot(),
                option.quantity()));
      }
      entity.addItem(itemEntity);
    }
    var saved = checkoutQuoteRepository.save(entity);
    return map(saved);
  }

  @Override
  public Optional<CheckoutQuoteSnapshot> findValidByStoreIdAndQuoteId(UUID storeId, UUID quoteId) {
    return checkoutQuoteRepository
        .findByIdAndStoreIdAndExpiresAtAfter(quoteId, storeId, OffsetDateTime.now())
        .map(this::map);
  }

  private CheckoutQuoteSnapshot map(CheckoutQuote entity) {
    return new CheckoutQuoteSnapshot(
        entity.getId(),
        entity.getStoreId(),
        entity.getCustomerId(),
        entity.getFulfillmentType(),
        entity.getAddressId(),
        entity.getSubtotalAmount(),
        entity.getDeliveryFeeAmount(),
        entity.getTotalAmount(),
        entity.getItems().stream()
            .map(
                item ->
                    new CheckoutQuoteItemSnapshot(
                        item.getProductId(),
                        item.getProductNameSnapshot(),
                        item.getUnitPriceSnapshot(),
                        item.getQuantity(),
                        item.getNotes(),
                        item.getOptions().stream()
                            .map(
                                option ->
                                    new CheckoutQuoteOptionSnapshot(
                                        option.getOptionNameSnapshot(),
                                        option.getExtraPriceSnapshot(),
                                        option.getQuantity()))
                            .toList()))
            .toList(),
        entity.getExpiresAt());
  }
}
