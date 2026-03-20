package com.kfood.merchant.app;

import com.kfood.merchant.api.StoreHoursResponse;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.Comparator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  StoreBusinessHourRepository.class,
  CurrentTenantProvider.class
})
public class GetStoreHoursUseCase {

  private final StoreRepository storeRepository;
  private final StoreBusinessHourRepository storeBusinessHourRepository;
  private final CurrentTenantProvider currentTenantProvider;

  public GetStoreHoursUseCase(
      StoreRepository storeRepository,
      StoreBusinessHourRepository storeBusinessHourRepository,
      CurrentTenantProvider currentTenantProvider) {
    this.storeRepository = storeRepository;
    this.storeBusinessHourRepository = storeBusinessHourRepository;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional(readOnly = true)
  public StoreHoursResponse execute() {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    var hours =
        storeBusinessHourRepository.findByStoreId(storeId).stream()
            .sorted(Comparator.comparingInt(item -> item.getDayOfWeek().getValue()))
            .map(StoreHoursMapper::toResponse)
            .toList();

    return new StoreHoursResponse(store.getHoursVersion(), hours);
  }
}
