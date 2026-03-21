package com.kfood.merchant.app;

import com.kfood.merchant.api.StoreHourRequest;
import com.kfood.merchant.api.UpdateStoreHoursRequest;
import com.kfood.merchant.api.UpdateStoreHoursResponse;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  StoreBusinessHourRepository.class,
  CurrentTenantProvider.class,
  StoreOperationalGuard.class
})
public class UpdateStoreHoursUseCase {

  private final StoreRepository storeRepository;
  private final StoreBusinessHourRepository storeBusinessHourRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public UpdateStoreHoursUseCase(
      StoreRepository storeRepository,
      StoreBusinessHourRepository storeBusinessHourRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreOperationalGuard storeOperationalGuard) {
    this.storeRepository = storeRepository;
    this.storeBusinessHourRepository = storeBusinessHourRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeOperationalGuard = storeOperationalGuard;
  }

  @Transactional
  public UpdateStoreHoursResponse execute(UpdateStoreHoursRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    validate(request.hours());

    storeBusinessHourRepository.deleteAllByStoreId(storeId);

    var newHours = request.hours().stream().map(hour -> toEntity(store, hour)).toList();
    storeBusinessHourRepository.saveAll(newHours);

    store.incrementHoursVersion();
    storeRepository.saveAndFlush(store);

    return new UpdateStoreHoursResponse(true, store.getHoursVersion());
  }

  private void validate(List<StoreHourRequest> hours) {
    var usedDays = EnumSet.noneOf(DayOfWeek.class);

    for (var hour : hours) {
      if (!usedDays.add(hour.dayOfWeek())) {
        throw new InvalidStoreHoursException("Duplicated dayOfWeek: " + hour.dayOfWeek());
      }

      if (hour.closed()) {
        if (hour.openTime() != null || hour.closeTime() != null) {
          throw new InvalidStoreHoursException(
              "Closed day must not define openTime or closeTime: " + hour.dayOfWeek());
        }
        continue;
      }

      if (hour.openTime() == null || hour.closeTime() == null) {
        throw new InvalidStoreHoursException(
            "Open day must define openTime and closeTime: " + hour.dayOfWeek());
      }

      if (!hour.openTime().isBefore(hour.closeTime())) {
        throw new InvalidStoreHoursException(
            "openTime must be before closeTime: " + hour.dayOfWeek());
      }
    }
  }

  private StoreBusinessHour toEntity(
      com.kfood.merchant.infra.persistence.Store store, StoreHourRequest hour) {
    if (hour.closed()) {
      return StoreBusinessHour.closed(store, hour.dayOfWeek());
    }

    return StoreBusinessHour.open(store, hour.dayOfWeek(), hour.openTime(), hour.closeTime());
  }
}
