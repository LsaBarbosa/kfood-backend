package com.kfood.customer.api;

import com.kfood.customer.app.UpsertCustomerUseCase;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/public/stores/{slug}/customers")
public class PublicCustomerController {

  private final ObjectProvider<UpsertCustomerUseCase> upsertCustomerUseCaseProvider;

  public PublicCustomerController(
      ObjectProvider<UpsertCustomerUseCase> upsertCustomerUseCaseProvider) {
    this.upsertCustomerUseCaseProvider = upsertCustomerUseCaseProvider;
  }

  @PostMapping
  public CustomerResponse upsert(
      @PathVariable String slug, @Valid @RequestBody UpsertCustomerRequest request) {
    return upsertCustomerUseCase().execute(slug, request);
  }

  private UpsertCustomerUseCase upsertCustomerUseCase() {
    return upsertCustomerUseCaseProvider.getObject();
  }
}
