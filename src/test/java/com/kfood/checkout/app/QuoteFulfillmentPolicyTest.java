package com.kfood.checkout.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteFulfillmentPolicyTest {

  private final CustomerAddressRepository customerAddressRepository =
      mock(CustomerAddressRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final QuoteFulfillmentPolicy policy =
      new QuoteFulfillmentPolicy(customerAddressRepository, deliveryZoneRepository);

  @Test
  void shouldNotRequireAddressForPickup() {
    var store = store("loja-do-bairro");
    var customer = customer(store);

    var result =
        policy.resolve(store, customer, FulfillmentType.PICKUP, null, new BigDecimal("20.00"), 1);

    assertThat(result.deliveryFee()).isEqualByComparingTo("0.00");
    assertThat(result.validatedAddressId()).isNull();
    assertThat(result.estimatedPreparationMinutes()).isEqualTo(20);
    assertThat(result.messages()).containsExactly("Pickup at the store.");
  }

  @Test
  void shouldRequireValidAddressForDelivery() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var addressId = UUID.randomUUID();

    when(customerAddressRepository.findByIdAndCustomerId(addressId, customer.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                policy.resolve(
                    store,
                    customer,
                    FulfillmentType.DELIVERY,
                    addressId,
                    new BigDecimal("40.00"),
                    1))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void shouldReturnDifferentFinalFeeForPickupAndDelivery() {
    var store = store("loja-do-bairro");
    var customer = customer(store);
    var address = address(customer, "Centro");
    var zone = deliveryZone(store, "Centro", new BigDecimal("6.50"));

    when(customerAddressRepository.findByIdAndCustomerId(address.getId(), customer.getId()))
        .thenReturn(Optional.of(address));
    when(deliveryZoneRepository.findByStoreIdAndZoneNameIgnoreCaseAndActiveTrue(
            store.getId(), "Centro"))
        .thenReturn(Optional.of(zone));

    var pickup =
        policy.resolve(store, customer, FulfillmentType.PICKUP, null, new BigDecimal("40.00"), 1);
    var delivery =
        policy.resolve(
            store, customer, FulfillmentType.DELIVERY, address.getId(), new BigDecimal("40.00"), 1);

    assertThat(pickup.deliveryFee()).isEqualByComparingTo("0.00");
    assertThat(delivery.deliveryFee()).isEqualByComparingTo("6.50");
    assertThat(new BigDecimal("40.00").add(pickup.deliveryFee())).isEqualByComparingTo("40.00");
    assertThat(new BigDecimal("40.00").add(delivery.deliveryFee())).isEqualByComparingTo("46.50");
  }

  @Test
  void shouldRejectDeliveryWhenAddressIdIsMissing() {
    var store = store("loja-do-bairro");
    var customer = customer(store);

    assertThatThrownBy(
            () ->
                policy.resolve(
                    store, customer, FulfillmentType.DELIVERY, null, new BigDecimal("40.00"), 1))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
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

  private CustomerAddress address(Customer customer, String district) {
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
        true);
  }

  private DeliveryZone deliveryZone(Store store, String zoneName, BigDecimal feeAmount) {
    return new DeliveryZone(
        UUID.randomUUID(), store, zoneName, feeAmount, new BigDecimal("25.00"), true);
  }
}
