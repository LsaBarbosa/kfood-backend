package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.catalog.app.availability.CatalogProductAvailabilityValidator;
import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.checkout.app.CheckoutQuoteItemSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.checkout.app.StoreCheckoutRulesValidator;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultCheckoutCriticalValidationServiceTest {

  private final StoreCheckoutRulesValidator storeCheckoutRulesValidator =
      mock(StoreCheckoutRulesValidator.class);
  private final CatalogProductRepository catalogProductRepository =
      mock(CatalogProductRepository.class);
  private final CatalogProductAvailabilityValidator catalogProductAvailabilityValidator =
      mock(CatalogProductAvailabilityValidator.class);
  private final CustomerRepository customerRepository = mock(CustomerRepository.class);
  private final CustomerAddressRepository customerAddressRepository =
      mock(CustomerAddressRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final DefaultCheckoutCriticalValidationService service =
      new DefaultCheckoutCriticalValidationService(
          storeCheckoutRulesValidator,
          catalogProductRepository,
          catalogProductAvailabilityValidator,
          customerRepository,
          customerAddressRepository,
          deliveryZoneRepository);

  @Test
  void shouldRevalidateQuoteSuccessfully() {
    var store = store();
    var customer = customer(store);
    var product = product(store, true, false);
    var quote = pickupQuote(store.getId(), customer.getId(), product.getId());

    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));
    when(catalogProductRepository.findAllByStoreIdAndIdIn(eq(store.getId()), any()))
        .thenReturn(List.of(product));

    service.revalidate(store, quote);

    verify(storeCheckoutRulesValidator).ensureStoreOperational(store);
    verify(storeCheckoutRulesValidator).ensureStoreWithinBusinessHours(store);
    verify(catalogProductAvailabilityValidator).ensureAvailableNow(product, store.getTimezone());
  }

  @Test
  void shouldRejectWhenCustomerIsMissing() {
    var store = store();
    var customerId = UUID.randomUUID();
    var quote = pickupQuote(store.getId(), customerId, UUID.randomUUID());
    when(customerRepository.findByIdAndStoreId(customerId, store.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.revalidate(store, quote))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Customer not found for this store.")
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void shouldRejectWhenAnyProductIsMissing() {
    var store = store();
    var customer = customer(store);
    var quote = pickupQuote(store.getId(), customer.getId(), UUID.randomUUID());
    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));
    when(catalogProductRepository.findAllByStoreIdAndIdIn(eq(store.getId()), any()))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.revalidate(store, quote))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Product is not available for checkout.")
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CATALOG_ITEM_UNAVAILABLE);
  }

  @Test
  void shouldRejectWhenProductIsInactiveOrPaused() {
    var store = store();
    var customer = customer(store);
    var inactiveProduct = product(store, false, true);
    var quote = pickupQuote(store.getId(), customer.getId(), inactiveProduct.getId());
    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));
    when(catalogProductRepository.findAllByStoreIdAndIdIn(eq(store.getId()), any()))
        .thenReturn(List.of(inactiveProduct));

    assertThatThrownBy(() -> service.revalidate(store, quote))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Product is not available for checkout.")
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CATALOG_ITEM_UNAVAILABLE);
  }

  @Test
  void shouldRejectWhenProductIsPausedEvenIfActive() {
    var store = store();
    var customer = customer(store);
    var pausedProduct = product(store, true, true);
    var quote = pickupQuote(store.getId(), customer.getId(), pausedProduct.getId());
    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));
    when(catalogProductRepository.findAllByStoreIdAndIdIn(eq(store.getId()), any()))
        .thenReturn(List.of(pausedProduct));

    assertThatThrownBy(() -> service.revalidate(store, quote))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Product is not available for checkout.")
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CATALOG_ITEM_UNAVAILABLE);
  }

  private CheckoutQuoteSnapshot pickupQuote(UUID storeId, UUID customerId, UUID productId) {
    return new CheckoutQuoteSnapshot(
        UUID.randomUUID(),
        storeId,
        customerId,
        com.kfood.order.domain.FulfillmentType.PICKUP,
        null,
        new BigDecimal("40.00"),
        BigDecimal.ZERO,
        new BigDecimal("40.00"),
        List.of(
            new CheckoutQuoteItemSnapshot(
                productId, "Pizza Calabresa", new BigDecimal("40.00"), 1, null, List.of())),
        OffsetDateTime.now().plusMinutes(10));
  }

  private Store store() {
    return new Store(
        UUID.randomUUID(),
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }

  private Customer customer(Store store) {
    return new Customer(UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com");
  }

  private CatalogProduct product(Store store, boolean active, boolean paused) {
    return new CatalogProduct(
        UUID.randomUUID(),
        store,
        new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 1, true),
        "Pizza Calabresa",
        "Pizza com calabresa",
        new BigDecimal("40.00"),
        null,
        1,
        active,
        paused);
  }
}
