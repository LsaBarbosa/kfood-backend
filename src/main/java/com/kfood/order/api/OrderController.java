package com.kfood.order.api;

import com.kfood.identity.app.Roles;
import com.kfood.order.app.ListOrdersQuery;
import com.kfood.order.app.ListOrdersUseCase;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import java.time.LocalDate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {

  private final ObjectProvider<ListOrdersUseCase> listOrdersUseCaseProvider;

  public OrderController(ObjectProvider<ListOrdersUseCase> listOrdersUseCaseProvider) {
    this.listOrdersUseCaseProvider = listOrdersUseCaseProvider;
  }

  @GetMapping
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public ListOrdersResponse list(
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dateTo,
      @RequestParam(required = false) FulfillmentType fulfillmentType,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return listOrdersUseCaseProvider
        .getObject()
        .execute(new ListOrdersQuery(status, dateFrom, dateTo, fulfillmentType), pageable);
  }
}
