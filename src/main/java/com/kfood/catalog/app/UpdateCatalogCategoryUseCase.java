package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogCategoryResponse;
import com.kfood.catalog.api.UpdateCatalogCategoryRequest;
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
public class UpdateCatalogCategoryUseCase {

  private final StoreRepository storeRepository;
  private final CatalogCategoryRepository catalogCategoryRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public UpdateCatalogCategoryUseCase(
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
  public CatalogCategoryResponse execute(UUID categoryId, UpdateCatalogCategoryRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store.getId(), store.getStatus());

    var category =
        catalogCategoryRepository
            .findByIdAndStoreId(categoryId, storeId)
            .orElseThrow(() -> new CatalogCategoryNotFoundException(categoryId));

    var name = request.name().trim();
    if (catalogCategoryRepository.existsByStoreIdAndNameAndIdNot(storeId, name, categoryId)) {
      throw new CatalogCategoryAlreadyExistsException(name);
    }

    category.changeName(name);
    category.changeSortOrder(request.sortOrder());

    return CatalogCategoryMapper.toResponse(catalogCategoryRepository.saveAndFlush(category));
  }
}
