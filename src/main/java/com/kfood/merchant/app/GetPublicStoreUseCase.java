package com.kfood.merchant.app;

import com.kfood.merchant.api.PublicStoreResponse;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import java.util.Comparator;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  StoreBusinessHourRepository.class,
  DeliveryZoneRepository.class
})
public class GetPublicStoreUseCase {

  private final StoreRepository storeRepository;
  private final StoreBusinessHourRepository storeBusinessHourRepository;
  private final DeliveryZoneRepository deliveryZoneRepository;

  public GetPublicStoreUseCase(
      StoreRepository storeRepository,
      StoreBusinessHourRepository storeBusinessHourRepository,
      DeliveryZoneRepository deliveryZoneRepository) {
    this.storeRepository = storeRepository;
    this.storeBusinessHourRepository = storeBusinessHourRepository;
    this.deliveryZoneRepository = deliveryZoneRepository;
  }

  @Transactional(readOnly = true)
  public PublicStoreResponse execute(String slug) {
    var normalizedSlug = normalize(slug);
    var store =
        storeRepository
            .findBySlug(normalizedSlug)
            .orElseThrow(() -> new StoreSlugNotFoundException(normalizedSlug));

    var hours =
        storeBusinessHourRepository.findByStoreId(store.getId()).stream()
            .sorted(Comparator.comparingInt(item -> item.getDayOfWeek().getValue()))
            .map(PublicStoreMapper::toHourResponse)
            .toList();

    var deliveryZones =
        deliveryZoneRepository
            .findAllByStoreIdAndActiveTrueOrderByZoneNameAsc(store.getId())
            .stream()
            .map(PublicStoreMapper::toDeliveryZoneResponse)
            .toList();

    return PublicStoreMapper.toResponse(store, hours, deliveryZones);
  }

  private String normalize(String slug) {
    return Objects.requireNonNull(slug, "slug is required").trim();
  }
}
