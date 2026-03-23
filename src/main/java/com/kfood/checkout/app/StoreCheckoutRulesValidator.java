package com.kfood.checkout.app;

import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(StoreBusinessHourRepository.class)
public class StoreCheckoutRulesValidator {

  private final StoreBusinessHourRepository storeBusinessHourRepository;
  private final Clock clock;

  public StoreCheckoutRulesValidator(StoreBusinessHourRepository storeBusinessHourRepository) {
    this(storeBusinessHourRepository, Clock.systemUTC());
  }

  StoreCheckoutRulesValidator(
      StoreBusinessHourRepository storeBusinessHourRepository, Clock clock) {
    this.storeBusinessHourRepository = storeBusinessHourRepository;
    this.clock = clock;
  }

  public void ensureStoreOperational(Store store) {
    if (!store.isActive()) {
      throw new StoreNotActiveException(store.getId(), store.getStatus());
    }
  }

  public void ensureStoreWithinBusinessHours(Store store) {
    var currentDateTime = clock.instant().atZone(ZoneId.of(store.getTimezone()));
    var currentDayOfWeek = currentDateTime.getDayOfWeek();
    var currentTime = currentDateTime.toLocalTime();

    var openNow = false;
    for (var hour :
        storeBusinessHourRepository.findAllByStoreIdAndDayOfWeek(store.getId(), currentDayOfWeek)) {
      if (hour.isClosed()) {
        continue;
      }
      if (hour.getOpenTime() == null || hour.getCloseTime() == null) {
        continue;
      }
      if (!currentTime.isBefore(hour.getOpenTime()) && currentTime.isBefore(hour.getCloseTime())) {
        openNow = true;
        break;
      }
    }

    if (!openNow) {
      throw new BusinessException(
          ErrorCode.STORE_OUTSIDE_BUSINESS_HOURS,
          "Store is outside business hours.",
          HttpStatus.UNPROCESSABLE_CONTENT);
    }
  }
}
