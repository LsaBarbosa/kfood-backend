package com.kfood.checkout.api;

import com.kfood.checkout.app.CalculateCheckoutQuoteUseCase;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/public/stores/{slug}/checkout")
public class PublicCheckoutController {

  private final ObjectProvider<CalculateCheckoutQuoteUseCase> calculateCheckoutQuoteUseCaseProvider;

  public PublicCheckoutController(
      ObjectProvider<CalculateCheckoutQuoteUseCase> calculateCheckoutQuoteUseCaseProvider) {
    this.calculateCheckoutQuoteUseCaseProvider = calculateCheckoutQuoteUseCaseProvider;
  }

  @PostMapping("/quote")
  public QuoteCheckoutResponse quote(
      @PathVariable String slug, @Valid @RequestBody QuoteCheckoutRequest request) {
    return calculateCheckoutQuoteUseCase().execute(slug, request.toCommand());
  }

  private CalculateCheckoutQuoteUseCase calculateCheckoutQuoteUseCase() {
    return calculateCheckoutQuoteUseCaseProvider.getObject();
  }
}
