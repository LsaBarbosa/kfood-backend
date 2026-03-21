package com.kfood.merchant.api;

import com.kfood.merchant.app.GetPublicStoreUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/public/stores")
public class PublicStoreController {

  private final ObjectProvider<GetPublicStoreUseCase> getPublicStoreUseCaseProvider;

  public PublicStoreController(
      ObjectProvider<GetPublicStoreUseCase> getPublicStoreUseCaseProvider) {
    this.getPublicStoreUseCaseProvider = getPublicStoreUseCaseProvider;
  }

  @GetMapping("/{slug}")
  public PublicStoreResponse getBySlug(@PathVariable String slug) {
    return getPublicStoreUseCase().execute(slug);
  }

  private GetPublicStoreUseCase getPublicStoreUseCase() {
    return getPublicStoreUseCaseProvider.getObject();
  }
}
