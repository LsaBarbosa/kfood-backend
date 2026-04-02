package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.util.UUID;
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
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class StoreRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private StoreRepository storeRepository;

  @Test
  @DisplayName("should persist valid store")
  void shouldPersistValidStore() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo",
            StoreCategory.PIZZARIA,
            new StoreAddress("25000-000", "Rua Central", "100", "Centro", "Mage", "RJ"));

    var savedStore = storeRepository.saveAndFlush(store);

    assertThat(savedStore.getId()).isNotNull();
    assertThat(savedStore.getStatus()).isEqualTo(StoreStatus.SETUP);
    assertThat(savedStore.isCashPaymentEnabled()).isFalse();
    assertThat(savedStore.getCategory()).isEqualTo(StoreCategory.PIZZARIA);
    assertThat(savedStore.getAddress().getZipCode()).isEqualTo("25000000");
    assertThat(savedStore.getAddress().getStreet()).isEqualTo("Rua Central");
    assertThat(savedStore.getCreatedAt()).isNotNull();
    assertThat(savedStore.getUpdatedAt()).isNotNull();
    assertThat(storeRepository.findBySlug("loja-do-bairro")).isPresent();
    assertThat(storeRepository.existsBySlug("loja-do-bairro")).isTrue();
  }
}
