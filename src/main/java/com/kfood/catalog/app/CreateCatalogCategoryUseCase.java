package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogCategoryResponse;
import com.kfood.catalog.api.CreateCatalogCategoryRequest;
import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
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
  CurrentTenantProvider.class,
  StoreOperationalGuard.class
})
public class CreateCatalogCategoryUseCase {

  private final StoreRepository storeRepository;
  private final CatalogCategoryRepository catalogCategoryRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public CreateCatalogCategoryUseCase(
      StoreRepository storeRepository,
      CatalogCategoryRepository catalogCategoryRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreOperationalGuard storeOperationalGuard) {
    this.storeRepository = storeRepository;
    this.catalogCategoryRepository = catalogCategoryRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeOperationalGuard = storeOperationalGuard;
  }

  @Transactional
  public CatalogCategoryResponse execute(CreateCatalogCategoryRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    var name = request.name().trim();
    if (catalogCategoryRepository.existsByStoreIdAndName(storeId, name)) {
      throw new CatalogCategoryAlreadyExistsException(name);
    }

    var category = new CatalogCategory(UUID.randomUUID(), store, name, request.sortOrder(), true);

    return CatalogCategoryMapper.toResponse(catalogCategoryRepository.saveAndFlush(category));
  }
}
