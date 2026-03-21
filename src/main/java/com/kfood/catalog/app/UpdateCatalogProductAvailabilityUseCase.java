package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogProductAvailabilityResponse;
import com.kfood.catalog.api.CatalogProductAvailabilityWindowRequest;
import com.kfood.catalog.api.UpdateCatalogProductAvailabilityRequest;
import com.kfood.catalog.infra.persistence.CatalogProductAvailabilityWindow;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  CatalogProductRepository.class,
  CurrentTenantProvider.class,
  StoreOperationalGuard.class
})
public class UpdateCatalogProductAvailabilityUseCase {

  private final StoreRepository storeRepository;
  private final CatalogProductRepository catalogProductRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public UpdateCatalogProductAvailabilityUseCase(
      StoreRepository storeRepository,
      CatalogProductRepository catalogProductRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreOperationalGuard storeOperationalGuard) {
    this.storeRepository = storeRepository;
    this.catalogProductRepository = catalogProductRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeOperationalGuard = storeOperationalGuard;
  }

  @Transactional
  public CatalogProductAvailabilityResponse execute(
      UUID productId, UpdateCatalogProductAvailabilityRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    var product =
        catalogProductRepository
            .findDetailedByIdAndStoreId(productId, storeId)
            .orElseThrow(() -> new CatalogProductNotFoundException(productId));

    validate(request.windows());

    var windows = new ArrayList<CatalogProductAvailabilityWindow>();
    for (var window : request.windows()) {
      windows.add(
          new CatalogProductAvailabilityWindow(
              UUID.randomUUID(),
              product,
              window.dayOfWeek(),
              window.startTime(),
              window.endTime(),
              window.active() == null || window.active()));
    }

    product.replaceAvailabilityWindows(windows);

    return CatalogProductAvailabilityMapper.toResponse(
        catalogProductRepository.saveAndFlush(product));
  }

  private void validate(java.util.List<CatalogProductAvailabilityWindowRequest> windows) {
    var activeWindows =
        windows.stream()
            .filter(window -> window.active() == null || window.active())
            .sorted(
                Comparator.comparing(CatalogProductAvailabilityWindowRequest::dayOfWeek)
                    .thenComparing(CatalogProductAvailabilityWindowRequest::startTime))
            .toList();

    CatalogProductAvailabilityWindowRequest previous = null;
    for (var window : activeWindows) {
      if (!window.startTime().isBefore(window.endTime())) {
        throw new InvalidCatalogProductAvailabilityException("startTime must be before endTime");
      }

      if (previous != null
          && previous.dayOfWeek() == window.dayOfWeek()
          && window.startTime().isBefore(previous.endTime())) {
        throw new InvalidCatalogProductAvailabilityException(
            "Overlapping availability windows for dayOfWeek: " + window.dayOfWeek());
      }

      previous = window;
    }

    for (var window : windows) {
      if (!window.startTime().isBefore(window.endTime())) {
        throw new InvalidCatalogProductAvailabilityException("startTime must be before endTime");
      }
    }
  }
}
