package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogProductResponse;
import com.kfood.catalog.api.CreateCatalogProductRequest;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogProduct;
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
public class CreateCatalogProductUseCase {

  private final StoreRepository storeRepository;
  private final CatalogCategoryRepository catalogCategoryRepository;
  private final CatalogProductRepository catalogProductRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public CreateCatalogProductUseCase(
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
  public CatalogProductResponse execute(CreateCatalogProductRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    var category =
        catalogCategoryRepository
            .findByIdAndStoreId(request.categoryId(), storeId)
            .orElseThrow(() -> new CatalogCategoryNotFoundException(request.categoryId()));

    var product =
        new CatalogProduct(
            UUID.randomUUID(),
            store,
            category,
            request.name().trim(),
            request.description().trim(),
            request.basePrice(),
            request.imageUrl(),
            request.sortOrder(),
            request.active(),
            request.paused());

    return CatalogProductMapper.toResponse(catalogProductRepository.saveAndFlush(product));
  }
}
