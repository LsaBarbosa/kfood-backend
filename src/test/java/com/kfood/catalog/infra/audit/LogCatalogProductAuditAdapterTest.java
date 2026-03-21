package com.kfood.catalog.infra.audit;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LogCatalogProductAuditAdapterTest {

  private final LogCatalogProductAuditAdapter adapter = new LogCatalogProductAuditAdapter();

  @Test
  void shouldRecordProductPauseChangedWithoutThrowing() {
    assertThatCode(
            () ->
                adapter.recordProductPauseChanged(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    true,
                    "Ingredient unavailable",
                    UUID.randomUUID()))
        .doesNotThrowAnyException();
  }
}
