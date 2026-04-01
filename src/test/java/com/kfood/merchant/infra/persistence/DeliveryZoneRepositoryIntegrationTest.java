package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.math.BigDecimal;
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
class DeliveryZoneRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private StoreRepository storeRepository;

  @Autowired private DeliveryZoneRepository deliveryZoneRepository;

  @Test
  @DisplayName("should persist valid delivery zone")
  void shouldPersistValidDeliveryZone() {
    var store =
        storeRepository.saveAndFlush(
            new Store(
                UUID.randomUUID(),
                "Loja do Bairro",
                "loja-do-bairro",
                "45.723.174/0001-10",
                "21999990000",
                "America/Sao_Paulo"));
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);

    var savedZone = deliveryZoneRepository.saveAndFlush(zone);

    assertThat(savedZone.getId()).isNotNull();
    assertThat(savedZone.getCreatedAt()).isNotNull();
    assertThat(savedZone.getUpdatedAt()).isNotNull();
    assertThat(deliveryZoneRepository.existsByStoreId(store.getId())).isTrue();
    assertThat(deliveryZoneRepository.findByStoreIdAndZoneName(store.getId(), "Centro"))
        .isPresent();
    assertThat(deliveryZoneRepository.findAllByStoreIdOrderByZoneNameAsc(store.getId())).hasSize(1);
  }
}
