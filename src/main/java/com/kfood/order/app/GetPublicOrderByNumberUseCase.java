package com.kfood.order.app;

import com.kfood.order.app.port.OrderQueryPort;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean(OrderQueryPort.class)
public class GetPublicOrderByNumberUseCase {

  private final OrderQueryPort orderQueryPort;

  public GetPublicOrderByNumberUseCase(OrderQueryPort orderQueryPort) {
    this.orderQueryPort = orderQueryPort;
  }

  @Transactional(readOnly = true)
  public PublicOrderLookupOutput execute(String slug, String orderNumber) {
    var normalizedSlug = normalize(slug);
    var normalizedOrderNumber = normalize(orderNumber);
    return orderQueryPort.getPublicOrderLookup(normalizedSlug, normalizedOrderNumber);
  }

  private String normalize(String value) {
    return Objects.requireNonNull(value, "value is required").trim();
  }
}
