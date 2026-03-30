package com.kfood.merchant.api;

import com.kfood.merchant.app.GetPublicStoreMenuUseCase;
import com.kfood.merchant.app.GetPublicStoreUseCase;
import com.kfood.merchant.app.PublicDeliveryZoneOutput;
import com.kfood.merchant.app.PublicStoreHourOutput;
import com.kfood.merchant.app.PublicStoreMenuCategoryOutput;
import com.kfood.merchant.app.PublicStoreMenuOptionGroupOutput;
import com.kfood.merchant.app.PublicStoreMenuOptionItemOutput;
import com.kfood.merchant.app.PublicStoreMenuOutput;
import com.kfood.merchant.app.PublicStoreMenuProductOutput;
import com.kfood.merchant.app.PublicStoreOutput;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/public/stores")
public class PublicStoreController {

  private final ObjectProvider<GetPublicStoreUseCase> getPublicStoreUseCaseProvider;
  private final ObjectProvider<GetPublicStoreMenuUseCase> getPublicStoreMenuUseCaseProvider;

  public PublicStoreController(
      ObjectProvider<GetPublicStoreUseCase> getPublicStoreUseCaseProvider,
      ObjectProvider<GetPublicStoreMenuUseCase> getPublicStoreMenuUseCaseProvider) {
    this.getPublicStoreUseCaseProvider = getPublicStoreUseCaseProvider;
    this.getPublicStoreMenuUseCaseProvider = getPublicStoreMenuUseCaseProvider;
  }

  @GetMapping("/{slug}")
  public PublicStoreResponse getBySlug(@PathVariable String slug) {
    return toResponse(getPublicStoreUseCase().execute(slug));
  }

  @GetMapping("/{slug}/menu")
  public PublicStoreMenuResponse getMenuBySlug(@PathVariable String slug) {
    return toResponse(getPublicStoreMenuUseCase().execute(slug));
  }

  private GetPublicStoreUseCase getPublicStoreUseCase() {
    return getPublicStoreUseCaseProvider.getObject();
  }

  private GetPublicStoreMenuUseCase getPublicStoreMenuUseCase() {
    return getPublicStoreMenuUseCaseProvider.getObject();
  }

  private PublicStoreResponse toResponse(PublicStoreOutput output) {
    return new PublicStoreResponse(
        output.slug(),
        output.name(),
        output.status(),
        output.phone(),
        output.hours().stream().map(this::toResponse).toList(),
        output.deliveryZones().stream().map(this::toResponse).toList());
  }

  private PublicStoreHourResponse toResponse(PublicStoreHourOutput output) {
    return new PublicStoreHourResponse(
        output.dayOfWeek(), output.openTime(), output.closeTime(), output.closed());
  }

  private PublicDeliveryZoneResponse toResponse(PublicDeliveryZoneOutput output) {
    return new PublicDeliveryZoneResponse(
        output.zoneName(), output.feeAmount(), output.minOrderAmount());
  }

  private PublicStoreMenuResponse toResponse(PublicStoreMenuOutput output) {
    return new PublicStoreMenuResponse(output.categories().stream().map(this::toResponse).toList());
  }

  private PublicStoreMenuCategoryResponse toResponse(PublicStoreMenuCategoryOutput output) {
    return new PublicStoreMenuCategoryResponse(
        output.id(), output.name(), output.products().stream().map(this::toResponse).toList());
  }

  private PublicStoreMenuProductResponse toResponse(PublicStoreMenuProductOutput output) {
    return new PublicStoreMenuProductResponse(
        output.id(),
        output.name(),
        output.description(),
        output.basePrice(),
        output.imageUrl(),
        output.paused(),
        output.optionGroups().stream().map(this::toResponse).toList());
  }

  private PublicStoreMenuOptionGroupResponse toResponse(PublicStoreMenuOptionGroupOutput output) {
    return new PublicStoreMenuOptionGroupResponse(
        output.id(),
        output.name(),
        output.minSelect(),
        output.maxSelect(),
        output.required(),
        output.items().stream().map(this::toResponse).toList());
  }

  private PublicStoreMenuOptionItemResponse toResponse(PublicStoreMenuOptionItemOutput output) {
    return new PublicStoreMenuOptionItemResponse(
        output.id(), output.name(), output.extraPrice(), output.sortOrder());
  }
}
