package com.kfood.order.infra.adapter;

import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.app.ListOrdersOutput;
import com.kfood.order.app.ListOrdersQuery;
import com.kfood.order.app.OrderDetailOutput;
import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.app.PublicOrderLookupOutput;
import com.kfood.order.app.port.OrderQueryPort;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderItem;
import com.kfood.order.infra.persistence.SalesOrderItemOption;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class JpaOrderQueryAdapter implements OrderQueryPort {

  private final SalesOrderRepository salesOrderRepository;
  private final StoreRepository storeRepository;

  public JpaOrderQueryAdapter(
      SalesOrderRepository salesOrderRepository, StoreRepository storeRepository) {
    this.salesOrderRepository = salesOrderRepository;
    this.storeRepository = storeRepository;
  }

  @Override
  public ListOrdersOutput listOperationalOrders(
      UUID storeId, ListOrdersQuery query, OffsetDateTime now, Pageable pageable) {
    var page =
        salesOrderRepository.findOperationalQueue(
            storeId,
            query.status(),
            query.fulfillmentType(),
            query.dateFrom() == null
                ? null
                : query.dateFrom().atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            query.dateTo() == null
                ? null
                : query.dateTo().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            now,
            pageable);

    return new ListOrdersOutput(
        page.getContent().stream().map(this::toItem).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        pageable.getSort().stream()
            .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
            .toList());
  }

  @Override
  public OrderDetailOutput getOrderDetail(UUID storeId, UUID orderId) {
    var order =
        salesOrderRepository
            .findDetailedByIdAndStoreId(orderId, storeId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    return toOrderDetail(order);
  }

  @Override
  public PublicOrderLookupOutput getPublicOrderLookup(String slug, String orderNumber) {
    var store =
        storeRepository.findBySlug(slug).orElseThrow(() -> new StoreSlugNotFoundException(slug));
    var order =
        salesOrderRepository
            .findByStoreIdAndOrderNumber(store.getId(), orderNumber)
            .orElseThrow(() -> new OrderNotFoundException(orderNumber));

    return new PublicOrderLookupOutput(
        order.getOrderNumber(),
        order.getStatus(),
        order.getPaymentStatusSnapshot(),
        order.getFulfillmentType(),
        order.getSubtotalAmount(),
        order.getDeliveryFeeAmount(),
        order.getTotalAmount(),
        order.getCreatedAt(),
        order.getScheduledFor());
  }

  private ListOrdersOutput.Item toItem(SalesOrder order) {
    return new ListOrdersOutput.Item(
        order.getId(),
        order.getOrderNumber(),
        order.getStatus(),
        order.getPaymentStatusSnapshot(),
        order.getCustomer().getName(),
        order.getTotalAmount(),
        order.getCreatedAt());
  }

  private OrderDetailOutput toOrderDetail(SalesOrder order) {
    return new OrderDetailOutput(
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
        new OrderDetailOutput.Customer(
            order.getCustomer().getId(),
            order.getCustomer().getName(),
            order.getCustomer().getPhone(),
            order.getCustomer().getEmail()),
        order.hasDeliveryAddressSnapshot()
            ? new OrderDetailOutput.Address(
                order.getDeliveryAddressLabel(),
                order.getDeliveryAddressZipCode(),
                order.getDeliveryAddressStreet(),
                order.getDeliveryAddressNumber(),
                order.getDeliveryAddressDistrict(),
                order.getDeliveryAddressCity(),
                order.getDeliveryAddressState(),
                order.getDeliveryAddressComplement())
            : null,
        new OrderDetailOutput.Payment(
            order.getPaymentMethodSnapshot(), order.getPaymentStatusSnapshot()),
        order.getItems().stream().map(this::toItemDetail).toList());
  }

  private OrderDetailOutput.Item toItemDetail(SalesOrderItem item) {
    return new OrderDetailOutput.Item(
        item.getId(),
        item.getProductId(),
        item.getProductNameSnapshot(),
        item.getUnitPriceSnapshot(),
        item.getQuantity(),
        item.getTotalItemAmount(),
        item.getNotes(),
        item.getOptions().stream().map(this::toItemOptionDetail).toList());
  }

  private OrderDetailOutput.ItemOption toItemOptionDetail(SalesOrderItemOption option) {
    return new OrderDetailOutput.ItemOption(
        option.getId(),
        option.getOptionNameSnapshot(),
        option.getExtraPriceSnapshot(),
        option.getQuantity(),
        option.getTotalExtraAmount());
  }
}
