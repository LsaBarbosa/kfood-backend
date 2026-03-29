package com.kfood.order.infra.adapter;

import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.app.AssignOrderNumberService;
import com.kfood.order.app.CreatePublicOrderCommand;
import com.kfood.order.app.CreatePublicOrderOutput;
import com.kfood.order.app.port.PublicOrderCommandPort;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderItem;
import com.kfood.order.infra.persistence.SalesOrderItemOption;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JpaPublicOrderCommandAdapter implements PublicOrderCommandPort {

  private final StoreRepository storeRepository;
  private final com.kfood.customer.infra.persistence.CustomerRepository customerRepository;
  private final com.kfood.customer.infra.persistence.CustomerAddressRepository
      customerAddressRepository;
  private final SalesOrderRepository salesOrderRepository;
  private final AssignOrderNumberService assignOrderNumberService;
  private final Clock clock;

  public JpaPublicOrderCommandAdapter(
      StoreRepository storeRepository,
      com.kfood.customer.infra.persistence.CustomerRepository customerRepository,
      com.kfood.customer.infra.persistence.CustomerAddressRepository customerAddressRepository,
      SalesOrderRepository salesOrderRepository,
      AssignOrderNumberService assignOrderNumberService,
      Clock clock) {
    this.storeRepository = storeRepository;
    this.customerRepository = customerRepository;
    this.customerAddressRepository = customerAddressRepository;
    this.salesOrderRepository = salesOrderRepository;
    this.assignOrderNumberService = assignOrderNumberService;
    this.clock = clock;
  }

  @Override
  public Optional<StoreReference> findStoreBySlug(String slug) {
    return storeRepository.findBySlug(slug).map(store -> new StoreReference(store.getId()));
  }

  @Override
  public CreatePublicOrderOutput createOrder(
      UUID storeId, CreatePublicOrderCommand command, CheckoutQuoteSnapshot quote) {
    var store = storeRepository.findById(storeId).orElseThrow();
    var customer =
        customerRepository
            .findByIdAndStoreId(command.customerId(), storeId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Customer not found for this store.",
                        HttpStatus.NOT_FOUND));
    var address =
        command.fulfillmentType() == FulfillmentType.PICKUP
            ? null
            : customerAddressRepository
                .findByIdAndCustomerId(command.addressId(), customer.getId())
                .orElseThrow(
                    () ->
                        new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "Address not found for this customer.",
                            HttpStatus.NOT_FOUND));

    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            command.fulfillmentType(),
            command.paymentMethod(),
            quote.subtotalAmount(),
            quote.deliveryFeeAmount(),
            quote.totalAmount(),
            null,
            command.notes());
    if (address != null) {
      order.defineDeliveryAddressSnapshot(address);
    }
    try {
      order.defineSchedule(command.scheduledFor(), clock);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "scheduledFor must be in the future.",
          HttpStatus.BAD_REQUEST);
    }
    for (var itemSnapshot : quote.items()) {
      var item =
          SalesOrderItem.create(
              UUID.randomUUID(),
              itemSnapshot.productId(),
              itemSnapshot.productNameSnapshot(),
              itemSnapshot.unitPriceSnapshot(),
              itemSnapshot.quantity(),
              itemSnapshot.notes());
      for (var optionSnapshot : itemSnapshot.options()) {
        item.addOption(
            SalesOrderItemOption.create(
                UUID.randomUUID(),
                optionSnapshot.optionNameSnapshot(),
                optionSnapshot.extraPriceSnapshot(),
                optionSnapshot.quantity()));
      }
      order.addItem(item);
    }

    assignOrderNumberService.assignIfMissing(order);
    var saved = salesOrderRepository.save(order);
    return new CreatePublicOrderOutput(
        saved.getId(),
        saved.getOrderNumber(),
        saved.getStatus(),
        saved.getPaymentStatusSnapshot(),
        saved.getSubtotalAmount(),
        saved.getDeliveryFeeAmount(),
        saved.getTotalAmount(),
        saved.getCreatedAt());
  }
}
