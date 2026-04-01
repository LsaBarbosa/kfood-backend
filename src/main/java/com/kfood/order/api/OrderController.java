package com.kfood.order.api;

import com.kfood.identity.app.Roles;
import com.kfood.order.app.CancelOrderCommand;
import com.kfood.order.app.CancelOrderUseCase;
import com.kfood.order.app.GetOrderDetailUseCase;
import com.kfood.order.app.ListOrdersQuery;
import com.kfood.order.app.ListOrdersUseCase;
import com.kfood.order.app.OrderDetailOutput;
import com.kfood.order.app.UpdateOrderStatusCommand;
import com.kfood.order.app.UpdateOrderStatusUseCase;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.app.CreateOrderPixPaymentCommand;
import com.kfood.payment.app.CreateOrderPixPaymentUseCase;
import com.kfood.payment.app.UpdatePaymentStatusCommand;
import com.kfood.payment.app.UpdatePaymentStatusUseCase;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {

  private final ObjectProvider<ListOrdersUseCase> listOrdersUseCaseProvider;
  private final ObjectProvider<GetOrderDetailUseCase> getOrderDetailUseCaseProvider;
  private final ObjectProvider<UpdateOrderStatusUseCase> updateOrderStatusUseCaseProvider;
  private final ObjectProvider<CancelOrderUseCase> cancelOrderUseCaseProvider;
  private final ObjectProvider<CreateOrderPixPaymentUseCase> createOrderPixPaymentUseCaseProvider;
  private final ObjectProvider<UpdatePaymentStatusUseCase> updatePaymentStatusUseCaseProvider;

  public OrderController(
      ObjectProvider<ListOrdersUseCase> listOrdersUseCaseProvider,
      ObjectProvider<GetOrderDetailUseCase> getOrderDetailUseCaseProvider,
      ObjectProvider<UpdateOrderStatusUseCase> updateOrderStatusUseCaseProvider,
      ObjectProvider<CancelOrderUseCase> cancelOrderUseCaseProvider,
      ObjectProvider<CreateOrderPixPaymentUseCase> createOrderPixPaymentUseCaseProvider,
      ObjectProvider<UpdatePaymentStatusUseCase> updatePaymentStatusUseCaseProvider) {
    this.listOrdersUseCaseProvider = listOrdersUseCaseProvider;
    this.getOrderDetailUseCaseProvider = getOrderDetailUseCaseProvider;
    this.updateOrderStatusUseCaseProvider = updateOrderStatusUseCaseProvider;
    this.cancelOrderUseCaseProvider = cancelOrderUseCaseProvider;
    this.createOrderPixPaymentUseCaseProvider = createOrderPixPaymentUseCaseProvider;
    this.updatePaymentStatusUseCaseProvider = updatePaymentStatusUseCaseProvider;
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
    var result =
        listOrdersUseCaseProvider
            .getObject()
            .execute(new ListOrdersQuery(status, dateFrom, dateTo, fulfillmentType), pageable);
    return new ListOrdersResponse(
        result.items().stream()
            .map(
                item ->
                    new ListOrdersResponseItem(
                        item.id(),
                        item.orderNumber(),
                        item.status(),
                        item.paymentStatusSnapshot(),
                        item.customerName(),
                        item.totalAmount(),
                        item.createdAt()))
            .toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages(),
        result.sort());
  }

  @GetMapping("/{orderId}")
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public OrderDetailResponse detail(@PathVariable UUID orderId) {
    return toOrderDetailResponse(getOrderDetailUseCaseProvider.getObject().execute(orderId));
  }

  @PatchMapping("/{orderId}/status")
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public UpdateOrderStatusResponse updateStatus(
      @PathVariable UUID orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
    var result =
        updateOrderStatusUseCaseProvider
            .getObject()
            .execute(orderId, new UpdateOrderStatusCommand(request.newStatus(), request.reason()));
    return new UpdateOrderStatusResponse(
        result.id(),
        result.previousStatus(),
        result.newStatus(),
        result.changedAt(),
        result.changedBy());
  }

  @PatchMapping("/payments/{paymentId}/status")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public UpdateOrderPaymentStatusResponse updatePaymentStatus(
      @PathVariable UUID paymentId, @Valid @RequestBody UpdateOrderPaymentStatusRequest request) {
    var result =
        updatePaymentStatusUseCaseProvider
            .getObject()
            .execute(new UpdatePaymentStatusCommand(paymentId, request.newStatus()));
    return new UpdateOrderPaymentStatusResponse(
        result.paymentId(),
        result.orderId(),
        result.paymentStatus(),
        result.orderPaymentStatusSnapshot());
  }

  @PostMapping("/{orderId}/cancel")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CancelOrderResponse cancel(
      @PathVariable UUID orderId, @Valid @RequestBody CancelOrderRequest request) {
    var result =
        cancelOrderUseCaseProvider
            .getObject()
            .execute(orderId, new CancelOrderCommand(request.reason()));
    return new CancelOrderResponse(
        result.id(), result.status(), result.canceledAt(), result.reason());
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

  private OrderDetailResponse toOrderDetailResponse(OrderDetailOutput output) {
    return new OrderDetailResponse(
        output.id(),
        output.orderNumber(),
        output.status(),
        output.fulfillmentType(),
        output.subtotalAmount(),
        output.deliveryFeeAmount(),
        output.totalAmount(),
        output.notes(),
        output.scheduledFor(),
        output.createdAt(),
        output.updatedAt(),
        new OrderDetailResponse.CustomerDetail(
            output.customer().id(),
            output.customer().name(),
            output.customer().phone(),
            output.customer().email()),
        output.address() == null
            ? null
            : new OrderDetailResponse.AddressDetail(
                output.address().label(),
                output.address().zipCode(),
                output.address().street(),
                output.address().number(),
                output.address().district(),
                output.address().city(),
                output.address().state(),
                output.address().complement()),
        new OrderDetailResponse.PaymentDetail(
            output.payment().paymentMethodSnapshot(), output.payment().paymentStatusSnapshot()),
        output.items().stream()
            .map(
                item ->
                    new OrderDetailResponse.ItemDetail(
                        item.id(),
                        item.productId(),
                        item.productNameSnapshot(),
                        item.unitPriceSnapshot(),
                        item.quantity(),
                        item.totalItemAmount(),
                        item.notes(),
                        item.options().stream()
                            .map(
                                option ->
                                    new OrderDetailResponse.ItemOptionDetail(
                                        option.id(),
                                        option.optionNameSnapshot(),
                                        option.extraPriceSnapshot(),
                                        option.quantity(),
                                        option.totalExtraAmount()))
                            .toList()))
            .toList());
  }
}
