package com.kfood.order.app;

import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.api.PublicOrderLookupResponse;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({StoreRepository.class, SalesOrderRepository.class})
public class GetPublicOrderByNumberUseCase {

  private final StoreRepository storeRepository;
  private final SalesOrderRepository salesOrderRepository;

  public GetPublicOrderByNumberUseCase(
      StoreRepository storeRepository, SalesOrderRepository salesOrderRepository) {
    this.storeRepository = storeRepository;
    this.salesOrderRepository = salesOrderRepository;
  }

  @Transactional(readOnly = true)
  public PublicOrderLookupResponse execute(String slug, String orderNumber) {
    var normalizedSlug = normalize(slug);
    var normalizedOrderNumber = normalize(orderNumber);

    var store =
        storeRepository
            .findBySlug(normalizedSlug)
            .orElseThrow(() -> new StoreSlugNotFoundException(normalizedSlug));

    var order =
        salesOrderRepository
            .findByStoreIdAndOrderNumber(store.getId(), normalizedOrderNumber)
            .orElseThrow(() -> new OrderNotFoundException(normalizedOrderNumber));

    return new PublicOrderLookupResponse(
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

  private String normalize(String value) {
    return Objects.requireNonNull(value, "value is required").trim();
  }
}
