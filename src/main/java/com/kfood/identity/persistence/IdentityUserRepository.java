package com.kfood.identity.persistence;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityUserRepository extends JpaRepository<IdentityUserEntity, UUID> {

  @EntityGraph(attributePaths = "roles")
  Optional<IdentityUserEntity> findByEmail(String email);

  @EntityGraph(attributePaths = "roles")
  Optional<IdentityUserEntity> findDetailedById(UUID id);

  @EntityGraph(attributePaths = "roles")
  List<IdentityUserEntity> findAllByStoreIdOrderByEmailAsc(UUID storeId);
}
