package com.kfood.checkout.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.app.availability.CatalogProductAvailabilityValidator;
import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionItem;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalculateCheckoutQuoteUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CustomerRepository customerRepository = mock(CustomerRepository.class);
  private final CustomerAddressRepository customerAddressRepository =
      mock(CustomerAddressRepository.class);
  private final CatalogProductRepository catalogProductRepository =
      mock(CatalogProductRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final CatalogProductAvailabilityValidator catalogProductAvailabilityValidator =
      mock(CatalogProductAvailabilityValidator.class);
  private final StoreCheckoutRulesValidator storeCheckoutRulesValidator =
      mock(StoreCheckoutRulesValidator.class);
  private final CheckoutQuoteSnapshotGateway checkoutQuoteSnapshotGateway =
      mock(CheckoutQuoteSnapshotGateway.class);
  private final QuoteFulfillmentPolicy quoteFulfillmentPolicy =
      new QuoteFulfillmentPolicy(customerAddressRepository, deliveryZoneRepository);
  private final CalculateCheckoutQuoteUseCase useCase =
      new CalculateCheckoutQuoteUseCase(
          storeRepository,
          customerRepository,
          catalogProductRepository,
          catalogProductAvailabilityValidator,
          storeCheckoutRulesValidator,
          quoteFulfillmentPolicy,
          checkoutQuoteSnapshotGateway);

  @Test
  void shouldCalculateSubtotalCorrectly() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var product = product(store, new BigDecimal("42.00"));
    var command =
        new CalculateCheckoutQuoteCommand(
            customer.getId(),
            FulfillmentType.PICKUP,
            null,
            List.of(new CalculateCheckoutQuoteItemCommand(product.getId(), 2, null, List.of())));

    mockBase(store, customer, List.of(product));

    var response = useCase.execute("loja-do-bairro", command);

    assertThat(response.subtotalAmount()).isEqualByComparingTo("84.00");
    assertThat(response.deliveryFeeAmount()).isEqualByComparingTo("0.00");
    assertThat(response.totalAmount()).isEqualByComparingTo("84.00");
    assertThat(response.messages()).containsExactly("Pickup at the store.");
  }

  @Test
  void shouldApplyCorrectDeliveryFee() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var address = address(customer, true, "Centro");
    var product = product(store, new BigDecimal("42.00"));
    var zone = deliveryZone(store, "Centro", new BigDecimal("6.50"));
    var command =
        new CalculateCheckoutQuoteCommand(
            customer.getId(),
            FulfillmentType.DELIVERY,
            address.getId(),
            List.of(new CalculateCheckoutQuoteItemCommand(product.getId(), 1, null, List.of())));

    mockBase(store, customer, List.of(product));
    when(customerAddressRepository.findByIdAndCustomerId(address.getId(), customer.getId()))
        .thenReturn(Optional.of(address));
    when(deliveryZoneRepository.findByStoreIdAndZoneNameIgnoreCaseAndActiveTrue(
            store.getId(), "Centro"))
        .thenReturn(Optional.of(zone));

    var response = useCase.execute("loja-do-bairro", command);

    assertThat(response.subtotalAmount()).isEqualByComparingTo("42.00");
    assertThat(response.deliveryFeeAmount()).isEqualByComparingTo("6.50");
    assertThat(response.totalAmount()).isEqualByComparingTo("48.50");
    assertThat(response.messages()).containsExactly("Delivery to zone Centro.");
  }

  @Test
  void shouldNotApplyFeeForPickup() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var product = product(store, new BigDecimal("42.00"));
    var command =
        new CalculateCheckoutQuoteCommand(
            customer.getId(),
            FulfillmentType.PICKUP,
            null,
            List.of(new CalculateCheckoutQuoteItemCommand(product.getId(), 1, null, List.of())));

    mockBase(store, customer, List.of(product));

    var response = useCase.execute("loja-do-bairro", command);

    assertThat(response.deliveryFeeAmount()).isEqualByComparingTo("0.00");
    assertThat(response.totalAmount()).isEqualByComparingTo("42.00");
  }

  @Test
  void shouldCalculateTotalWithAddonsCorrectly() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var product = product(store, new BigDecimal("42.00"));
    var group =
        new CatalogOptionGroup(UUID.randomUUID(), product, "Borda recheada", 0, 1, false, true);
    var optionItem =
        new CatalogOptionItem(
            UUID.randomUUID(), group, "Catupiry", new BigDecimal("8.00"), true, 10);
    group.addItem(optionItem);
    addOptionGroup(product, group);
    var command =
        new CalculateCheckoutQuoteCommand(
            customer.getId(),
            FulfillmentType.PICKUP,
            null,
            List.of(
                new CalculateCheckoutQuoteItemCommand(
                    product.getId(),
                    2,
                    null,
                    List.of(new CalculateCheckoutQuoteItemOptionCommand(optionItem.getId(), 1)))));

    mockBase(store, customer, List.of(product));

    var response = useCase.execute("loja-do-bairro", command);

    assertThat(response.subtotalAmount()).isEqualByComparingTo("100.00");
    assertThat(response.totalAmount()).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldRejectAddressOutsideSupportedDeliveryArea() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var address = address(customer, true, "Bairro Distante");
    var product = product(store, new BigDecimal("42.00"));
    var command =
        new CalculateCheckoutQuoteCommand(
            customer.getId(),
            FulfillmentType.DELIVERY,
            address.getId(),
            List.of(new CalculateCheckoutQuoteItemCommand(product.getId(), 1, null, List.of())));

    mockBase(store, customer, List.of(product));
    when(customerAddressRepository.findByIdAndCustomerId(address.getId(), customer.getId()))
        .thenReturn(Optional.of(address));
    when(deliveryZoneRepository.findByStoreIdAndZoneNameIgnoreCaseAndActiveTrue(
            store.getId(), "Bairro Distante"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute("loja-do-bairro", command))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DELIVERY_ZONE_NOT_SUPPORTED);
  }

  @Test
  void shouldRejectWhenOrderBelowMinimum() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var address = address(customer, true, "Centro");
    var product = product(store, new BigDecimal("20.00"));
    var zone = deliveryZone(store, "Centro", new BigDecimal("6.50"));
    var command =
        new CalculateCheckoutQuoteCommand(
            customer.getId(),
            FulfillmentType.DELIVERY,
            address.getId(),
            List.of(new CalculateCheckoutQuoteItemCommand(product.getId(), 1, null, List.of())));

    mockBase(store, customer, List.of(product));
    when(customerAddressRepository.findByIdAndCustomerId(address.getId(), customer.getId()))
        .thenReturn(Optional.of(address));
    when(deliveryZoneRepository.findByStoreIdAndZoneNameIgnoreCaseAndActiveTrue(
            store.getId(), "Centro"))
        .thenReturn(Optional.of(zone));

    assertThatThrownBy(() -> useCase.execute("loja-do-bairro", command))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.MIN_ORDER_NOT_REACHED);
  }

  @Test
  void shouldCalculateDifferentTotalsForPickupAndDelivery() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var address = address(customer, true, "Centro");
    var product = product(store, new BigDecimal("40.00"));
    var zone = deliveryZone(store, "Centro", new BigDecimal("6.50"));
    var pickupCommand =
        new CalculateCheckoutQuoteCommand(
            customer.getId(),
            FulfillmentType.PICKUP,
            null,
            List.of(new CalculateCheckoutQuoteItemCommand(product.getId(), 1, null, List.of())));
    var deliveryCommand =
        new CalculateCheckoutQuoteCommand(
            customer.getId(),
            FulfillmentType.DELIVERY,
            address.getId(),
            List.of(new CalculateCheckoutQuoteItemCommand(product.getId(), 1, null, List.of())));

    mockBase(store, customer, List.of(product));
    when(customerAddressRepository.findByIdAndCustomerId(address.getId(), customer.getId()))
        .thenReturn(Optional.of(address));
    when(deliveryZoneRepository.findByStoreIdAndZoneNameIgnoreCaseAndActiveTrue(
            store.getId(), "Centro"))
        .thenReturn(Optional.of(zone));

    var pickupResponse = useCase.execute("loja-do-bairro", pickupCommand);
    var deliveryResponse = useCase.execute("loja-do-bairro", deliveryCommand);

    assertThat(pickupResponse.totalAmount()).isEqualByComparingTo("40.00");
    assertThat(deliveryResponse.totalAmount()).isEqualByComparingTo("46.50");
    assertThat(pickupResponse.estimatedPreparationMinutes()).isEqualTo(20);
    assertThat(deliveryResponse.estimatedPreparationMinutes()).isEqualTo(35);
  }

  @Test
  void shouldHandleDuplicatedProductsFromRepositoryResult() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var product = product(store, new BigDecimal("42.00"));
    var command =
        new CalculateCheckoutQuoteCommand(
            customer.getId(),
            FulfillmentType.PICKUP,
            null,
            List.of(new CalculateCheckoutQuoteItemCommand(product.getId(), 1, null, List.of())));

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));
    when(catalogProductRepository.findAllByStoreIdAndIdIn(store.getId(), List.of(product.getId())))
        .thenReturn(List.of(product, product));

    var response = useCase.execute("loja-do-bairro", command);

    assertThat(response.totalAmount()).isEqualByComparingTo("42.00");
  }

  @Test
  void shouldRejectWhenStoreDoesNotExist() {
    when(storeRepository.findBySlug("loja-inexistente")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.execute(
                    "loja-inexistente",
                    new CalculateCheckoutQuoteCommand(
                        UUID.randomUUID(), FulfillmentType.PICKUP, null, List.of())))
        .isInstanceOf(com.kfood.merchant.app.StoreSlugNotFoundException.class);
  }

  @Test
  void shouldRejectWhenCustomerDoesNotBelongToStore() {
    var store = store("loja-do-bairro");
    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(any(), eq(store.getId())))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.execute(
                    "loja-do-bairro",
                    new CalculateCheckoutQuoteCommand(
                        UUID.randomUUID(),
                        FulfillmentType.PICKUP,
                        null,
                        List.of(
                            new CalculateCheckoutQuoteItemCommand(
                                UUID.randomUUID(), 1, null, List.of())))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void shouldRejectWhenProductIsMissing() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var productId = UUID.randomUUID();
    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));
    when(catalogProductRepository.findAllByStoreIdAndIdIn(store.getId(), List.of(productId)))
        .thenReturn(List.of());

    assertThatThrownBy(
            () ->
                useCase.execute(
                    "loja-do-bairro",
                    new CalculateCheckoutQuoteCommand(
                        customer.getId(),
                        FulfillmentType.PICKUP,
                        null,
                        List.of(
                            new CalculateCheckoutQuoteItemCommand(productId, 1, null, List.of())))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void shouldRejectWhenOptionItemIsNotAvailableForProduct() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var product = product(store, new BigDecimal("42.00"));
    mockBase(store, customer, List.of(product));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    "loja-do-bairro",
                    new CalculateCheckoutQuoteCommand(
                        customer.getId(),
                        FulfillmentType.PICKUP,
                        null,
                        List.of(
                            new CalculateCheckoutQuoteItemCommand(
                                product.getId(),
                                1,
                                null,
                                List.of(
                                    new CalculateCheckoutQuoteItemOptionCommand(
                                        UUID.randomUUID(), 1)))))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CATALOG_ITEM_UNAVAILABLE);
  }

  @Test
  void shouldRejectWhenProductIsInactive() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var product = product(store, new BigDecimal("42.00"));
    product.deactivate();
    mockBase(store, customer, List.of(product));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    "loja-do-bairro",
                    new CalculateCheckoutQuoteCommand(
                        customer.getId(),
                        FulfillmentType.PICKUP,
                        null,
                        List.of(
                            new CalculateCheckoutQuoteItemCommand(
                                product.getId(), 1, null, List.of())))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CATALOG_ITEM_UNAVAILABLE);
  }

  @Test
  void shouldRejectWhenProductIsPaused() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var product = product(store, new BigDecimal("42.00"));
    product.pause();
    mockBase(store, customer, List.of(product));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    "loja-do-bairro",
                    new CalculateCheckoutQuoteCommand(
                        customer.getId(),
                        FulfillmentType.PICKUP,
                        null,
                        List.of(
                            new CalculateCheckoutQuoteItemCommand(
                                product.getId(), 1, null, List.of())))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CATALOG_ITEM_UNAVAILABLE);
  }

  @Test
  void shouldRejectWhenSelectedOptionsAreBelowMinimum() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var product = product(store, new BigDecimal("42.00"));
    var group = new CatalogOptionGroup(UUID.randomUUID(), product, "Extras", 1, 2, true, true);
    addOptionGroup(product, group);
    mockBase(store, customer, List.of(product));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    "loja-do-bairro",
                    new CalculateCheckoutQuoteCommand(
                        customer.getId(),
                        FulfillmentType.PICKUP,
                        null,
                        List.of(
                            new CalculateCheckoutQuoteItemCommand(
                                product.getId(), 1, null, List.of())))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void shouldRejectWhenSelectedOptionsExceedMaximum() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var product = product(store, new BigDecimal("42.00"));
    var group = new CatalogOptionGroup(UUID.randomUUID(), product, "Extras", 0, 1, false, true);
    var firstItem =
        new CatalogOptionItem(
            UUID.randomUUID(), group, "Catupiry", new BigDecimal("8.00"), true, 1);
    var secondItem =
        new CatalogOptionItem(UUID.randomUUID(), group, "Cheddar", new BigDecimal("7.00"), true, 2);
    group.addItem(firstItem);
    group.addItem(secondItem);
    addOptionGroup(product, group);
    mockBase(store, customer, List.of(product));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    "loja-do-bairro",
                    new CalculateCheckoutQuoteCommand(
                        customer.getId(),
                        FulfillmentType.PICKUP,
                        null,
                        List.of(
                            new CalculateCheckoutQuoteItemCommand(
                                product.getId(),
                                1,
                                null,
                                List.of(
                                    new CalculateCheckoutQuoteItemOptionCommand(
                                        firstItem.getId(), 1),
                                    new CalculateCheckoutQuoteItemOptionCommand(
                                        secondItem.getId(), 1)))))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void shouldIgnoreInactiveOptionGroupsAndItems() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var product = product(store, new BigDecimal("42.00"));
    var inactiveGroup =
        new CatalogOptionGroup(UUID.randomUUID(), product, "Inactive", 1, 1, true, false);
    var inactiveItem =
        new CatalogOptionItem(
            UUID.randomUUID(), inactiveGroup, "Ignored", new BigDecimal("9.00"), false, 1);
    inactiveGroup.addItem(inactiveItem);
    addOptionGroup(product, inactiveGroup);
    mockBase(store, customer, List.of(product));

    var response =
        useCase.execute(
            "loja-do-bairro",
            new CalculateCheckoutQuoteCommand(
                customer.getId(),
                FulfillmentType.PICKUP,
                null,
                List.of(
                    new CalculateCheckoutQuoteItemCommand(product.getId(), 1, null, List.of()))));

    assertThat(response.totalAmount()).isEqualByComparingTo("42.00");
  }

  private void mockBase(Store store, Customer customer, List<CatalogProduct> products) {
    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));
    when(catalogProductRepository.findAllByStoreIdAndIdIn(
            store.getId(), products.stream().map(CatalogProduct::getId).toList()))
        .thenReturn(products);
  }

  private Store store(String slug) {
    return new Store(
        UUID.randomUUID(),
        "Loja do Bairro",
        slug,
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }

  private Customer customer(Store store) {
    return new Customer(UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com");
  }

  private CustomerAddress address(Customer customer, boolean mainAddress, String district) {
    return new CustomerAddress(
        UUID.randomUUID(),
        customer,
        "Casa",
        "25000000",
        "Rua das Flores",
        "45",
        district,
        "Mage",
        "RJ",
        null,
        mainAddress);
  }

  private DeliveryZone deliveryZone(Store store, String zoneName, BigDecimal feeAmount) {
    return new DeliveryZone(
        UUID.randomUUID(), store, zoneName, feeAmount, new BigDecimal("25.00"), true);
  }

  private CatalogProduct product(Store store, BigDecimal basePrice) {
    return new CatalogProduct(
        UUID.randomUUID(),
        store,
        new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 1, true),
        "Pizza Calabresa",
        "Pizza com calabresa",
        basePrice,
        null,
        1,
        true,
        false);
  }

  private void addOptionGroup(CatalogProduct product, CatalogOptionGroup group) {
    try {
      var field = CatalogProduct.class.getDeclaredField("optionGroups");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      var groups = (List<CatalogOptionGroup>) field.get(product);
      groups.add(group);
    } catch (ReflectiveOperationException exception) {
      throw new RuntimeException(exception);
    }
  }
}
