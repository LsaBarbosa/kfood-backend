package com.kfood.identity.persistence;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "identity_user")
public class IdentityUserEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "store_id")
  private UUID storeId;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private UserStatus status;

  @OneToMany(
      mappedBy = "user",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private final Set<IdentityUserRoleEntity> roles = new LinkedHashSet<>();

  protected IdentityUserEntity() {}

  public IdentityUserEntity(
      UUID id, UUID storeId, String email, String passwordHash, UserStatus status) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.storeId = storeId;
    this.email = Objects.requireNonNull(email, "email is required");
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash is required");
    this.status = Objects.requireNonNull(status, "status is required");
  }

  public void replaceRoles(Set<UserRoleName> roleNames) {
    if (roleNames == null || roleNames.isEmpty()) {
      throw new IllegalArgumentException("User must have at least one role.");
    }

    roles.clear();
    roleNames.stream().distinct().map(this::newRole).forEach(roles::add);
  }

  private IdentityUserRoleEntity newRole(UserRoleName roleName) {
    return new IdentityUserRoleEntity(UUID.randomUUID(), this, storeId, roleName);
  }

  public UUID getId() {
    return id;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public UserStatus getStatus() {
    return status;
  }

  public Set<IdentityUserRoleEntity> getRoles() {
    return Collections.unmodifiableSet(roles);
  }
}
