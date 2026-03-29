package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptance;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreTermsAcceptanceMapperTest {

  @Test
  void shouldMapPostOutput() {
    var acceptance =
        new StoreTermsAcceptance(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            LegalDocumentType.TERMS_OF_USE,
            "2026.03",
            Instant.parse("2026-03-20T10:15:00Z"),
            "203.0.113.9");

    var response = StoreTermsAcceptanceMapper.toOutput(acceptance);

    assertThat(response.id()).isEqualTo(acceptance.getId());
    assertThat(response.documentType()).isEqualTo(LegalDocumentType.TERMS_OF_USE);
    assertThat(response.documentVersion()).isEqualTo("2026.03");
  }

  @Test
  void shouldMapHistoryItem() {
    var acceptance =
        new StoreTermsAcceptance(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            LegalDocumentType.TERMS_OF_USE,
            "2026.03",
            Instant.parse("2026-03-20T10:15:00Z"),
            "203.0.113.9");

    var response = StoreTermsAcceptanceMapper.toHistoryItemOutput(acceptance);

    assertThat(response.id()).isEqualTo(acceptance.getId());
    assertThat(response.acceptedByUserId()).isEqualTo(acceptance.getAcceptedByUserId());
    assertThat(response.documentType()).isEqualTo(LegalDocumentType.TERMS_OF_USE);
    assertThat(response.documentVersion()).isEqualTo("2026.03");
  }
}
