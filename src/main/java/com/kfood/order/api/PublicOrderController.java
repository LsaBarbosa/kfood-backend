package com.kfood.order.api;

import com.kfood.order.app.CreatePublicOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/public/stores/{slug}/orders")
public class PublicOrderController {

  private final ObjectProvider<CreatePublicOrderService> createPublicOrderServiceProvider;

  public PublicOrderController(
      ObjectProvider<CreatePublicOrderService> createPublicOrderServiceProvider) {
    this.createPublicOrderServiceProvider = createPublicOrderServiceProvider;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreatePublicOrderResponse create(
      @PathVariable String slug,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CreatePublicOrderRequest request) {
    var createPublicOrderService = createPublicOrderServiceProvider.getIfAvailable();
    if (createPublicOrderService == null) {
      throw new IllegalStateException("CreatePublicOrderService is not available.");
    }
    return createPublicOrderService.create(slug, idempotencyKey, request);
  }
}
