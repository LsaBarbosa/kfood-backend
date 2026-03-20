package com.kfood.merchant.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, UUID> {

  boolean existsByStoreIdAndZoneName(UUID storeId, String zoneName);

  Optional<DeliveryZone> findByIdAndStoreId(UUID id, UUID storeId);

  Optional<DeliveryZone> findByStoreIdAndZoneName(UUID storeId, String zoneName);

  List<DeliveryZone> findAllByStoreIdOrderByZoneNameAsc(UUID storeId);

  boolean existsByStoreId(UUID storeId);

  boolean existsByStoreIdAndActiveTrue(UUID storeId);
}
