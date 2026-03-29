package com.kfood.order.infra.adapter;

import com.kfood.catalog.app.availability.CatalogProductAvailabilityValidator;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.checkout.app.QuoteFulfillmentPolicy;
import com.kfood.checkout.app.StoreCheckoutRulesValidator;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.app.port.OrderCheckoutValidationPort;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean({
  StoreCheckoutRulesValidator.class,
  CatalogProductAvailabilityValidator.class
})
public class JpaCheckoutCriticalValidationAdapter implements OrderCheckoutValidationPort {

  private final StoreRepository storeRepository;
  private final StoreCheckoutRulesValidator storeCheckoutRulesValidator;
  private final CatalogProductRepository catalogProductRepository;
  private final CatalogProductAvailabilityValidator catalogProductAvailabilityValidator;
  private final CustomerRepository customerRepository;
  private final QuoteFulfillmentPolicy quoteFulfillmentPolicy;

  public JpaCheckoutCriticalValidationAdapter(
      StoreRepository storeRepository,
      StoreCheckoutRulesValidator storeCheckoutRulesValidator,
      CatalogProductRepository catalogProductRepository,
      CatalogProductAvailabilityValidator catalogProductAvailabilityValidator,
      CustomerRepository customerRepository,
      CustomerAddressRepository customerAddressRepository,
      DeliveryZoneRepository deliveryZoneRepository) {
    this.storeRepository = storeRepository;
    this.storeCheckoutRulesValidator = storeCheckoutRulesValidator;
    this.catalogProductRepository = catalogProductRepository;
    this.catalogProductAvailabilityValidator = catalogProductAvailabilityValidator;
    this.customerRepository = customerRepository;
    this.quoteFulfillmentPolicy =
        new QuoteFulfillmentPolicy(customerAddressRepository, deliveryZoneRepository);
  }

  @Override
  public void revalidate(java.util.UUID storeId, CheckoutQuoteSnapshot quoteSnapshot) {
    var store =
        storeRepository
            .findById(storeId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Store not found for this order.",
                        HttpStatus.NOT_FOUND));
    storeCheckoutRulesValidator.ensureStoreOperational(store);
    storeCheckoutRulesValidator.ensureStoreWithinBusinessHours(store);
    var customer =
        customerRepository
            .findByIdAndStoreId(quoteSnapshot.customerId(), storeId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Customer not found for this store.",
                        HttpStatus.NOT_FOUND));
    var products =
        catalogProductRepository.findAllByStoreIdAndIdIn(
            storeId, quoteSnapshot.items().stream().map(item -> item.productId()).toList());
    var productsById =
        products.stream()
            .collect(java.util.stream.Collectors.toMap(p -> p.getId(), Function.identity()));
    if (productsById.size() != quoteSnapshot.items().size()) {
      throw new BusinessException(
          ErrorCode.CATALOG_ITEM_UNAVAILABLE,
          "Product is not available for checkout.",
          HttpStatus.UNPROCESSABLE_CONTENT);
    }
    for (var item : quoteSnapshot.items()) {
      var product = productsById.get(item.productId());
      if (!product.isActive() || product.isPaused()) {
        throw new BusinessException(
            ErrorCode.CATALOG_ITEM_UNAVAILABLE,
            "Product is not available for checkout.",
            HttpStatus.UNPROCESSABLE_CONTENT);
      }
      catalogProductAvailabilityValidator.ensureAvailableNow(product, store.getTimezone());
    }
    quoteFulfillmentPolicy.resolve(
        store,
        customer,
        quoteSnapshot.fulfillmentType(),
        quoteSnapshot.addressId(),
        quoteSnapshot.subtotalAmount(),
        quoteSnapshot.items().stream().mapToInt(item -> item.quantity()).sum());
  }
}
