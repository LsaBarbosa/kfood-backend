package com.kfood.identity.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityEntityCoverageTest {

  @Test
  void shouldRejectNullRoles() {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "owner@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);

    assertThatThrownBy(() -> user.replaceRoles(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User must have at least one role.");
  }

  @Test
  void shouldDistinctRolesAndExposeUnmodifiableView() {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "owner@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);

    user.replaceRoles(Set.of(UserRoleName.OWNER));

    assertThat(user.getId()).isNotNull();
    assertThat(user.getStoreId()).isNotNull();
    assertThat(user.getEmail()).isEqualTo("owner@kfood.local");
    assertThat(user.getPasswordHash()).isEqualTo("$2a$10$hash");
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.getRoles()).hasSize(1);
    assertThatThrownBy(() -> user.getRoles().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldBindUserAndRolesToStore() {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(), null, "owner@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));

    var storeId = UUID.randomUUID();
    user.bindToStore(storeId);

    assertThat(user.getStoreId()).isEqualTo(storeId);
    assertThat(user.getRoles())
        .extracting(IdentityUserRoleEntity::getStoreId)
        .containsOnly(storeId);
  }

  @Test
  void shouldRejectBindingUserToAnotherStore() {
    var firstStoreId = UUID.randomUUID();
    var secondStoreId = UUID.randomUUID();
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(), firstStoreId, "owner@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));

    assertThatThrownBy(() -> user.bindToStore(secondStoreId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("User is already bound to another store.");
  }

  @Test
  void shouldAllowBindingUserToSameStoreAgain() {
    var storeId = UUID.randomUUID();
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(), storeId, "owner@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));

    user.bindToStore(storeId);

    assertThat(user.getStoreId()).isEqualTo(storeId);
  }

  @Test
  void shouldRejectBindingRoleToAnotherStore() {
    var firstStoreId = UUID.randomUUID();
    var secondStoreId = UUID.randomUUID();
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            firstStoreId,
            "manager@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    var role =
        new IdentityUserRoleEntity(UUID.randomUUID(), user, firstStoreId, UserRoleName.MANAGER);

    assertThatThrownBy(() -> role.bindToStore(secondStoreId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Role is already bound to another store.");
  }

  @Test
  void shouldAllowBindingRoleToSameStoreAgain() {
    var storeId = UUID.randomUUID();
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(), storeId, "manager@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    var role = new IdentityUserRoleEntity(UUID.randomUUID(), user, storeId, UserRoleName.MANAGER);

    role.bindToStore(storeId);

    assertThat(role.getStoreId()).isEqualTo(storeId);
  }

  @Test
  void shouldConstructDefaultUserEntityViaReflection() throws Exception {
    Constructor<IdentityUserEntity> constructor = IdentityUserEntity.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  @Test
  void shouldHandleRoleEntityLifecycleAndGetters() throws Exception {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "manager@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    var role =
        new IdentityUserRoleEntity(
            UUID.randomUUID(), user, user.getStoreId(), UserRoleName.MANAGER);

    assertThat(role.getId()).isNotNull();
    assertThat(role.getUser()).isSameAs(user);
    assertThat(role.getStoreId()).isEqualTo(user.getStoreId());
    assertThat(role.getRoleName()).isEqualTo(UserRoleName.MANAGER);
    assertThat(role.getCreatedAt()).isNull();

    Method prePersist = IdentityUserRoleEntity.class.getDeclaredMethod("prePersist");
    prePersist.setAccessible(true);
    prePersist.invoke(role);

    assertThat(role.getCreatedAt()).isNotNull();

    Field createdAt = IdentityUserRoleEntity.class.getDeclaredField("createdAt");
    createdAt.setAccessible(true);
    var fixedInstant = Instant.parse("2026-01-01T00:00:00Z");
    createdAt.set(role, fixedInstant);

    prePersist.invoke(role);

    assertThat(role.getCreatedAt()).isEqualTo(fixedInstant);
  }

  @Test
  void shouldConstructDefaultRoleEntityViaReflection() throws Exception {
    Constructor<IdentityUserRoleEntity> constructor =
        IdentityUserRoleEntity.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }
}
