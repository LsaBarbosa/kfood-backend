package com.kfood.merchant.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StoreTermsAcceptanceContractDocumentationTest {

  private static final Path CONTRACT_PATH =
      Path.of("docs/api/merchant-store-terms-acceptance-contract.md");

  @Test
  void shouldPreventTermsAcceptanceContractMarkdownFromDriftingFromImplementedServerSideFields()
      throws IOException {
    var contract = Files.readString(CONTRACT_PATH, StandardCharsets.UTF_8);
    var postSection =
        sectionBetween(
            contract,
            "## POST `/v1/merchant/store/terms-acceptance`",
            "## GET `/v1/merchant/store/terms-acceptance/history`");
    var requestBlock = sectionBetween(postSection, "Request:", "Response `201 Created`:");
    var responseBlock = postSection.substring(postSection.indexOf("Response `201 Created`:"));

    assertThat(postSection)
        .contains("campos aceitos no request: `documentType`, `documentVersion`");
    assertThat(postSection).contains("`acceptedAt` e gerado no servidor");
    assertThat(postSection)
        .contains("o backend deriva o IP do cliente a partir dos metadados da requisicao HTTP");
    assertThat(postSection)
        .contains("o cliente nao controla nem envia `acceptedAt` ou `requestIp`");

    assertThat(requestBlock)
        .contains("\"documentType\": \"TERMS_OF_USE\"")
        .contains("\"documentVersion\": \"2026.03\"")
        .doesNotContain("\"acceptedAt\"")
        .doesNotContain("\"requestIp\"");

    assertThat(responseBlock).contains("\"acceptedAt\": \"2026-03-20T10:15:00Z\"");
  }

  private static String sectionBetween(String content, String startMarker, String endMarker) {
    var start = content.indexOf(startMarker);
    var end = content.indexOf(endMarker, start);
    assertThat(start).as("Missing marker: %s", startMarker).isGreaterThanOrEqualTo(0);
    assertThat(end).as("Missing marker: %s", endMarker).isGreaterThan(start);
    return content.substring(start, end);
  }
}
