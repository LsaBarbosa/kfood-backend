package com.kfood.identity.persistence;

import com.kfood.identity.domain.UserRoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "identity_user_role")
public class IdentityUserRoleEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private IdentityUserEntity user;

  @Column(name = "store_id")
  private UUID storeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "role_name", nullable = false)
  private UserRoleName roleName;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected IdentityUserRoleEntity() {}

  public IdentityUserRoleEntity(
      UUID id, IdentityUserEntity user, UUID storeId, UserRoleName roleName) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.user = Objects.requireNonNull(user, "user is required");
    this.storeId = storeId;
    this.roleName = Objects.requireNonNull(roleName, "roleName is required");
  }

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public IdentityUserEntity getUser() {
    return user;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public UserRoleName getRoleName() {
    return roleName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
