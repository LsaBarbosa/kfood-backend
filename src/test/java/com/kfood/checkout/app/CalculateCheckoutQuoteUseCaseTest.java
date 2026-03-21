package com.kfood.checkout.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.app.availability.CatalogProductAvailabilityValidator;
import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionItem;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.checkout.domain.FulfillmentType;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
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
  private final CalculateCheckoutQuoteUseCase useCase =
      new CalculateCheckoutQuoteUseCase(
          storeRepository,
          customerRepository,
          customerAddressRepository,
          catalogProductRepository,
          deliveryZoneRepository,
          catalogProductAvailabilityValidator,
          storeCheckoutRulesValidator);

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
