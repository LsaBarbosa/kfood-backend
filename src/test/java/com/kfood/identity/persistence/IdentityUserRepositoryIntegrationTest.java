package com.kfood.identity.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import jakarta.persistence.EntityManager;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class IdentityUserRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private IdentityUserRepository repository;

  @Autowired private EntityManager entityManager;

  @BeforeEach
  void setUpConstraints() {
    entityManager
        .createNativeQuery(
            """
            alter table identity_user
                add constraint chk_identity_user_status
                    check (status in ('ACTIVE', 'INACTIVE', 'LOCKED'))
            """)
        .executeUpdate();

    entityManager
        .createNativeQuery(
            """
            alter table identity_user_role
                add constraint chk_identity_user_role_name
                    check (role_name in ('OWNER', 'MANAGER', 'ATTENDANT', 'ADMIN'))
            """)
        .executeUpdate();
  }

  @Test
  @DisplayName("should persist user with valid role")
  void shouldPersistUserWithValidRole() {
    IdentityUserEntity user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            null,
            "admin@kfood.local",
            "hashed-password",
            UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.ADMIN));

    IdentityUserEntity saved = repository.saveAndFlush(user);
    entityManager.clear();

    IdentityUserEntity found = repository.findByEmail("admin@kfood.local").orElseThrow();

    assertThat(found.getId()).isEqualTo(saved.getId());
    assertThat(found.getStoreId()).isNull();
    assertThat(found.getRoles())
        .extracting(IdentityUserRoleEntity::getRoleName)
        .containsExactly(UserRoleName.ADMIN);
  }

  @Test
  @DisplayName("should reject invalid role at database level")
  void shouldRejectInvalidRoleAtDatabaseLevel() {
    UUID userId = UUID.randomUUID();

    entityManager
        .createNativeQuery(
            """
            insert into identity_user (id, store_id, email, password_hash, status, created_at, updated_at)
            values (:id, null, :email, :passwordHash, :status, current_timestamp, current_timestamp)
            """)
        .setParameter("id", userId)
        .setParameter("email", "invalid-role@kfood.local")
        .setParameter("passwordHash", "hashed-password")
        .setParameter("status", "ACTIVE")
        .executeUpdate();

    assertThatThrownBy(
            () -> {
              entityManager
                  .createNativeQuery(
                      """
                      insert into identity_user_role (id, user_id, store_id, role_name, created_at)
                      values (:id, :userId, null, :roleName, current_timestamp)
                      """)
                  .setParameter("id", UUID.randomUUID())
                  .setParameter("userId", userId)
                  .setParameter("roleName", "SUPER_ADMIN")
                  .executeUpdate();
              entityManager.flush();
            })
        .rootCause()
        .hasMessageContaining("chk_identity_user_role_name");
  }

  @Test
  @DisplayName("should reject user without role")
  void shouldRejectUserWithoutRole() {
    IdentityUserEntity user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "no-role@kfood.local",
            "hashed-password",
            UserStatus.ACTIVE);

    assertThatThrownBy(() -> user.replaceRoles(Set.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User must have at least one role.");
  }
}
