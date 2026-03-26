package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.domain.LegalDocumentType;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreTermsAcceptanceEntityTest {

  @Test
  void shouldExposeImmutableAcceptanceData() {
    var id = UUID.randomUUID();
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var acceptance =
        new StoreTermsAcceptance(
            id,
            storeId,
            userId,
            LegalDocumentType.TERMS_OF_USE,
            " 2026.03 ",
            Instant.parse("2026-03-20T13:15:00Z"),
            " 203.0.113.9 ");

    assertThat(acceptance.getId()).isEqualTo(id);
    assertThat(acceptance.getStoreId()).isEqualTo(storeId);
    assertThat(acceptance.getAcceptedByUserId()).isEqualTo(userId);
    assertThat(acceptance.getDocumentType()).isEqualTo(LegalDocumentType.TERMS_OF_USE);
    assertThat(acceptance.getDocumentVersion()).isEqualTo("2026.03");
    assertThat(acceptance.getAcceptedAt()).isEqualTo(Instant.parse("2026-03-20T13:15:00Z"));
    assertThat(acceptance.getRequestIp()).isEqualTo("203.0.113.9");
    assertThat(acceptance.getCreatedAt()).isNull();
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<StoreTermsAcceptance> constructor =
        StoreTermsAcceptance.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }
}
