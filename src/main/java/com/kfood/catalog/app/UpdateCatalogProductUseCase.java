package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogProductResponse;
import com.kfood.catalog.api.UpdateCatalogProductRequest;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  CatalogCategoryRepository.class,
  CatalogProductRepository.class,
  CurrentTenantProvider.class,
  StoreOperationalGuard.class
})
public class UpdateCatalogProductUseCase {

  private final StoreRepository storeRepository;
  private final CatalogCategoryRepository catalogCategoryRepository;
  private final CatalogProductRepository catalogProductRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public UpdateCatalogProductUseCase(
      StoreRepository storeRepository,
      CatalogCategoryRepository catalogCategoryRepository,
      CatalogProductRepository catalogProductRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreOperationalGuard storeOperationalGuard) {
    this.storeRepository = storeRepository;
    this.catalogCategoryRepository = catalogCategoryRepository;
    this.catalogProductRepository = catalogProductRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeOperationalGuard = storeOperationalGuard;
  }

  @Transactional
  public CatalogProductResponse execute(UUID productId, UpdateCatalogProductRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    var product =
        catalogProductRepository
            .findByIdAndStoreId(productId, storeId)
            .orElseThrow(() -> new CatalogProductNotFoundException(productId));

    var category =
        catalogCategoryRepository
            .findByIdAndStoreId(request.categoryId(), storeId)
            .orElseThrow(() -> new CatalogCategoryNotFoundException(request.categoryId()));

    product.changeCategory(category);
    product.changeName(request.name().trim());
    product.changeDescription(request.description().trim());
    product.changeBasePrice(request.basePrice());
    product.changeImageUrl(request.imageUrl());
    product.changeSortOrder(request.sortOrder());
    if (request.active()) {
      product.activate();
    } else {
      product.deactivate();
    }
    if (request.paused()) {
      product.pause();
    } else {
      product.resume();
    }

    return CatalogProductMapper.toResponse(catalogProductRepository.saveAndFlush(product));
  }
}
