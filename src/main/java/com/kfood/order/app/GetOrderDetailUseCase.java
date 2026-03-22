package com.kfood.order.app;

import com.kfood.order.api.OrderDetailResponse;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderItem;
import com.kfood.order.infra.persistence.SalesOrderItemOption;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({SalesOrderRepository.class, CurrentTenantProvider.class})
public class GetOrderDetailUseCase {

  private final SalesOrderRepository salesOrderRepository;
  private final CurrentTenantProvider currentTenantProvider;

  public GetOrderDetailUseCase(
      SalesOrderRepository salesOrderRepository, CurrentTenantProvider currentTenantProvider) {
    this.salesOrderRepository = salesOrderRepository;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional(readOnly = true)
  public OrderDetailResponse execute(UUID orderId) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var order =
        salesOrderRepository
            .findDetailedByIdAndStoreId(orderId, storeId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    return toResponse(order);
  }

  private OrderDetailResponse toResponse(SalesOrder order) {
    return new OrderDetailResponse(
        order.getId(),
        order.getOrderNumber(),
        order.getStatus(),
        order.getFulfillmentType(),
        order.getSubtotalAmount(),
        order.getDeliveryFeeAmount(),
        order.getTotalAmount(),
        order.getNotes(),
        order.getScheduledFor(),
        order.getCreatedAt(),
        order.getUpdatedAt(),
        new OrderDetailResponse.CustomerDetail(
            order.getCustomer().getId(),
            order.getCustomer().getName(),
            order.getCustomer().getPhone(),
            order.getCustomer().getEmail()),
        toAddress(order),
        new OrderDetailResponse.PaymentDetail(
            order.getPaymentMethod(), order.getPaymentStatusSnapshot()),
        order.getItems().stream().map(this::toItem).toList());
  }

  private OrderDetailResponse.AddressDetail toAddress(SalesOrder order) {
    if (!order.hasDeliveryAddressSnapshot()) {
      return null;
    }

    return new OrderDetailResponse.AddressDetail(
        order.getDeliveryAddressLabel(),
        order.getDeliveryAddressZipCode(),
        order.getDeliveryAddressStreet(),
        order.getDeliveryAddressNumber(),
        order.getDeliveryAddressDistrict(),
        order.getDeliveryAddressCity(),
        order.getDeliveryAddressState(),
        order.getDeliveryAddressComplement());
  }

  private OrderDetailResponse.ItemDetail toItem(SalesOrderItem item) {
    return new OrderDetailResponse.ItemDetail(
        item.getId(),
        item.getProductId(),
        item.getProductNameSnapshot(),
        item.getUnitPriceSnapshot(),
        item.getQuantity(),
        item.getTotalItemAmount(),
        item.getNotes(),
        item.getOptions().stream().map(this::toOption).toList());
  }

  private OrderDetailResponse.ItemOptionDetail toOption(SalesOrderItemOption option) {
    return new OrderDetailResponse.ItemOptionDetail(
        option.getId(),
        option.getOptionNameSnapshot(),
        option.getExtraPriceSnapshot(),
        option.getQuantity(),
        option.getTotalExtraAmount());
  }
}
