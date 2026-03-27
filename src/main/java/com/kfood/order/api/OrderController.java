package com.kfood.order.api;

import com.kfood.identity.app.Roles;
import com.kfood.order.app.CancelOrderUseCase;
import com.kfood.order.app.GetOrderDetailUseCase;
import com.kfood.order.app.ListOrdersQuery;
import com.kfood.order.app.ListOrdersUseCase;
import com.kfood.order.app.UpdateOrderStatusUseCase;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.app.CreateOrderPixPaymentCommand;
import com.kfood.payment.app.CreateOrderPixPaymentUseCase;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {

  private final ObjectProvider<ListOrdersUseCase> listOrdersUseCaseProvider;
  private final ObjectProvider<GetOrderDetailUseCase> getOrderDetailUseCaseProvider;
  private final ObjectProvider<UpdateOrderStatusUseCase> updateOrderStatusUseCaseProvider;
  private final ObjectProvider<CancelOrderUseCase> cancelOrderUseCaseProvider;
  private final ObjectProvider<CreateOrderPixPaymentUseCase> createOrderPixPaymentUseCaseProvider;

  public OrderController(
      ObjectProvider<ListOrdersUseCase> listOrdersUseCaseProvider,
      ObjectProvider<GetOrderDetailUseCase> getOrderDetailUseCaseProvider,
      ObjectProvider<UpdateOrderStatusUseCase> updateOrderStatusUseCaseProvider,
      ObjectProvider<CancelOrderUseCase> cancelOrderUseCaseProvider,
      ObjectProvider<CreateOrderPixPaymentUseCase> createOrderPixPaymentUseCaseProvider) {
    this.listOrdersUseCaseProvider = listOrdersUseCaseProvider;
    this.getOrderDetailUseCaseProvider = getOrderDetailUseCaseProvider;
    this.updateOrderStatusUseCaseProvider = updateOrderStatusUseCaseProvider;
    this.cancelOrderUseCaseProvider = cancelOrderUseCaseProvider;
    this.createOrderPixPaymentUseCaseProvider = createOrderPixPaymentUseCaseProvider;
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

  @GetMapping("/{orderId}")
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public OrderDetailResponse detail(@PathVariable UUID orderId) {
    return getOrderDetailUseCaseProvider.getObject().execute(orderId);
  }

  @PatchMapping("/{orderId}/status")
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public UpdateOrderStatusResponse updateStatus(
      @PathVariable UUID orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
    return updateOrderStatusUseCaseProvider.getObject().execute(orderId, request);
  }

  @PostMapping("/{orderId}/cancel")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CancelOrderResponse cancel(
      @PathVariable UUID orderId, @Valid @RequestBody CancelOrderRequest request) {
    return cancelOrderUseCaseProvider.getObject().execute(orderId, request);
  }

  @PostMapping("/{orderId}/payments/pix")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public CreatePixPaymentResponse createPixPayment(
      @PathVariable UUID orderId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CreatePixPaymentRequest request) {
    var result =
        createOrderPixPaymentUseCaseProvider
            .getObject()
            .execute(
                new CreateOrderPixPaymentCommand(
                    orderId, request.amount(), request.provider(), idempotencyKey));
    return new CreatePixPaymentResponse(
        result.paymentId(),
        result.orderId(),
        result.paymentMethod(),
        result.status(),
        result.providerReference(),
        result.qrCodePayload(),
        result.expiresAt());
  }
}
