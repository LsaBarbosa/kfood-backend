package com.kfood.merchant.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface StoreBusinessHourRepository extends JpaRepository<StoreBusinessHour, UUID> {

  List<StoreBusinessHour> findByStoreId(UUID storeId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from StoreBusinessHour hour where hour.store.id = :storeId")
  void deleteAllByStoreId(UUID storeId);

  boolean existsByStoreId(UUID storeId);
}
