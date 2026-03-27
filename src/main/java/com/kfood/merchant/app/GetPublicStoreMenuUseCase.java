package com.kfood.merchant.app;

import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionGroupRepository;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.api.PublicStoreMenuCategoryResponse;
import com.kfood.merchant.api.PublicStoreMenuResponse;
import com.kfood.merchant.infra.persistence.StoreRepository;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  CatalogCategoryRepository.class,
  CatalogOptionGroupRepository.class,
  CatalogProductRepository.class
})
public class GetPublicStoreMenuUseCase {

  private final StoreRepository storeRepository;
  private final CatalogCategoryRepository catalogCategoryRepository;
  private final CatalogOptionGroupRepository catalogOptionGroupRepository;
  private final CatalogProductRepository catalogProductRepository;

  public GetPublicStoreMenuUseCase(
      StoreRepository storeRepository,
      CatalogCategoryRepository catalogCategoryRepository,
      CatalogOptionGroupRepository catalogOptionGroupRepository,
      CatalogProductRepository catalogProductRepository) {
    this.storeRepository = storeRepository;
    this.catalogCategoryRepository = catalogCategoryRepository;
    this.catalogOptionGroupRepository = catalogOptionGroupRepository;
    this.catalogProductRepository = catalogProductRepository;
  }

  @Transactional(readOnly = true)
  public PublicStoreMenuResponse execute(String slug) {
    var normalizedSlug = normalize(slug);
    var store =
        storeRepository
            .findBySlug(normalizedSlug)
            .orElseThrow(() -> new StoreSlugNotFoundException(normalizedSlug));

    var categoriesById = new LinkedHashMap<java.util.UUID, PublicStoreMenuCategoryAccumulator>();
    var storeZoneId = ZoneId.of(store.getTimezone());
    var now = java.time.ZonedDateTime.now(storeZoneId);
    var products =
        catalogProductRepository.findAllVisibleForPublicMenu(
            store.getId(), now.getDayOfWeek(), now.toLocalTime());
    var optionGroupsByProductId = loadOptionGroupsByProductId(products);

    for (var product : products) {
      var category = product.getCategory();
      categoriesById
          .computeIfAbsent(
              category.getId(),
              ignored ->
                  new PublicStoreMenuCategoryAccumulator(category.getId(), category.getName()))
          .products()
          .add(
              PublicStoreMapper.toMenuProductResponse(
                  product,
                  optionGroupsByProductId.getOrDefault(product.getId(), java.util.List.of())));
    }

    return new PublicStoreMenuResponse(
        categoriesById.values().stream()
            .map(
                category ->
                    new PublicStoreMenuCategoryResponse(
                        category.id(), category.name(), java.util.List.copyOf(category.products())))
            .toList());
  }

  private String normalize(String slug) {
    return Objects.requireNonNull(slug, "slug is required").trim();
  }

  private java.util.Map<java.util.UUID, java.util.List<CatalogOptionGroup>>
      loadOptionGroupsByProductId(
          java.util.List<com.kfood.catalog.infra.persistence.CatalogProduct> products) {
    if (products.isEmpty()) {
      return java.util.Map.of();
    }

    var groupsByProductId =
        new java.util.LinkedHashMap<java.util.UUID, java.util.List<CatalogOptionGroup>>();
    var productIds =
        products.stream().map(com.kfood.catalog.infra.persistence.CatalogProduct::getId).toList();
    for (var group :
        catalogOptionGroupRepository.findAllByProduct_IdInAndActiveTrueOrderByProduct_IdAscIdAsc(
            productIds)) {
      groupsByProductId
          .computeIfAbsent(group.getProduct().getId(), ignored -> new java.util.ArrayList<>())
          .add(group);
    }
    return groupsByProductId;
  }

  private record PublicStoreMenuCategoryAccumulator(
      java.util.UUID id,
      String name,
      java.util.List<com.kfood.merchant.api.PublicStoreMenuProductResponse> products) {

    private PublicStoreMenuCategoryAccumulator(java.util.UUID id, String name) {
      this(id, name, new java.util.ArrayList<>());
    }
  }
}
