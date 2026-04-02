package com.kfood.identity.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.identity.persistence.IdentityUserRoleEntity;
import com.kfood.shared.config.PasswordSecurityConfig;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import jakarta.persistence.EntityManager;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@Import({
  TestJpaAuditingConfig.class,
  PasswordSecurityConfig.class,
  com.kfood.identity.infra.security.PasswordHashServiceImpl.class,
  CreateUserService.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class CreateUserServiceIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private CreateUserService createUserService;

  @Autowired private IdentityUserRepository identityUserRepository;

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
  @DisplayName("should persist password as hash and keep informed status")
  void shouldPersistPasswordAsHashAndKeepInformedStatus() {
    String rawPassword = "Senha@123";

    IdentityUserEntity created =
        createUserService.create(
            null,
            "admin@kfood.local",
            rawPassword,
            Set.of(UserRoleName.ADMIN),
            com.kfood.identity.domain.UserStatus.INACTIVE);

    entityManager.flush();
    entityManager.clear();

    IdentityUserEntity persisted = identityUserRepository.findById(created.getId()).orElseThrow();

    assertThat(persisted.getPasswordHash()).isNotBlank();
    assertThat(persisted.getPasswordHash()).isNotEqualTo(rawPassword);
    assertThat(persisted.getStatus()).isEqualTo(com.kfood.identity.domain.UserStatus.INACTIVE);
    assertThat(persisted.getRoles())
        .extracting(IdentityUserRoleEntity::getRoleName)
        .containsExactly(UserRoleName.ADMIN);
  }
}
