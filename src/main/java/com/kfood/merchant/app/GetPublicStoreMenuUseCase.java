package com.kfood.merchant.app;

import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.api.PublicStoreMenuCategoryResponse;
import com.kfood.merchant.api.PublicStoreMenuProductResponse;
import com.kfood.merchant.api.PublicStoreMenuResponse;
import com.kfood.merchant.infra.persistence.StoreRepository;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  CatalogCategoryRepository.class,
  CatalogProductRepository.class
})
public class GetPublicStoreMenuUseCase {

  private final StoreRepository storeRepository;
  private final CatalogCategoryRepository catalogCategoryRepository;
  private final CatalogProductRepository catalogProductRepository;

  public GetPublicStoreMenuUseCase(
      StoreRepository storeRepository,
      CatalogCategoryRepository catalogCategoryRepository,
      CatalogProductRepository catalogProductRepository) {
    this.storeRepository = storeRepository;
    this.catalogCategoryRepository = catalogCategoryRepository;
    this.catalogProductRepository = catalogProductRepository;
  }

  @Transactional(readOnly = true)
  public PublicStoreMenuResponse execute(String slug) {
    var normalizedSlug = normalize(slug);
    var store =
        storeRepository
            .findBySlug(normalizedSlug)
            .orElseThrow(() -> new StoreSlugNotFoundException(normalizedSlug));

    var storeId = store.getId();
    var activeCategories =
        catalogCategoryRepository
            .findAllByStoreIdAndActiveTrueOrderBySortOrderAscNameAsc(storeId)
            .stream()
            .filter(category -> category.getStore().getId().equals(storeId))
            .toList();
    var visibleCategoryIds = activeCategories.stream().map(category -> category.getId()).toList();

    var visibleProducts =
        catalogProductRepository
            .findAllByStoreIdAndActiveTrueAndPausedFalseOrderBySortOrderAscNameAsc(storeId)
            .stream()
            .filter(
                product -> belongsToStoreAndVisibleCategory(product, storeId, visibleCategoryIds))
            .toList();

    var productsByCategoryId =
        new LinkedHashMap<UUID, java.util.List<PublicStoreMenuProductResponse>>();
    for (var product : visibleProducts) {
      productsByCategoryId
          .computeIfAbsent(product.getCategory().getId(), ignored -> new java.util.ArrayList<>())
          .add(toProductResponse(product));
    }

    var categories =
        activeCategories.stream()
            .filter(category -> productsByCategoryId.containsKey(category.getId()))
            .map(
                category ->
                    new PublicStoreMenuCategoryResponse(
                        category.getId(),
                        category.getName(),
                        productsByCategoryId.get(category.getId())))
            .toList();

    return new PublicStoreMenuResponse(categories);
  }

  private boolean belongsToStoreAndVisibleCategory(
      CatalogProduct product, UUID storeId, java.util.List<UUID> visibleCategoryIds) {
    return product.getStore().getId().equals(storeId)
        && product.getCategory().getStore().getId().equals(storeId)
        && visibleCategoryIds.contains(product.getCategory().getId())
        && product.isActive()
        && !product.isPaused();
  }

  private PublicStoreMenuProductResponse toProductResponse(CatalogProduct product) {
    return new PublicStoreMenuProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getBasePrice(),
        product.getImageUrl(),
        product.isPaused());
  }

  private String normalize(String slug) {
    return Objects.requireNonNull(slug, "slug is required").trim();
  }
}
