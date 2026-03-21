package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogOptionGroupResponse;
import com.kfood.catalog.api.CreateCatalogOptionGroupRequest;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionGroupRepository;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  CatalogProductRepository.class,
  CatalogOptionGroupRepository.class,
  CurrentTenantProvider.class,
  StoreOperationalGuard.class
})
public class CreateCatalogOptionGroupUseCase {

  private final StoreRepository storeRepository;
  private final CatalogProductRepository catalogProductRepository;
  private final CatalogOptionGroupRepository catalogOptionGroupRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public CreateCatalogOptionGroupUseCase(
      StoreRepository storeRepository,
      CatalogProductRepository catalogProductRepository,
      CatalogOptionGroupRepository catalogOptionGroupRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreOperationalGuard storeOperationalGuard) {
    this.storeRepository = storeRepository;
    this.catalogProductRepository = catalogProductRepository;
    this.catalogOptionGroupRepository = catalogOptionGroupRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeOperationalGuard = storeOperationalGuard;
  }

  @Transactional
  public CatalogOptionGroupResponse execute(
      UUID productId, CreateCatalogOptionGroupRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    var product =
        catalogProductRepository
            .findByIdAndStoreId(productId, storeId)
            .orElseThrow(() -> new CatalogProductNotFoundException(productId));

    var minSelect = request.minSelect() == null ? 0 : request.minSelect();
    var maxSelect = request.maxSelect() == null ? 1 : request.maxSelect();

    if (maxSelect < minSelect) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "maxSelect must be greater than or equal to minSelect",
          HttpStatus.BAD_REQUEST);
    }

    var optionGroup =
        new CatalogOptionGroup(
            UUID.randomUUID(),
            product,
            request.name().trim(),
            minSelect,
            maxSelect,
            Boolean.TRUE.equals(request.required()),
            request.active() == null || request.active());

    return CatalogOptionGroupMapper.toResponse(
        catalogOptionGroupRepository.saveAndFlush(optionGroup));
  }
}
