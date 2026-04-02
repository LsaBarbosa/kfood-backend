package com.kfood.merchant.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicStoreContractDocumentationTest {

  private static final Path CONTRACT_PATH = Path.of("docs/api/public-store-contract.md");

  @Test
  void shouldPreventPublicStoreContractMarkdownFromDriftingFromImplementedPublicPayload()
      throws IOException {
    var contract = Files.readString(CONTRACT_PATH, StandardCharsets.UTF_8);
    var getSection = contract.substring(contract.indexOf("## GET `/v1/public/stores/{slug}`"));
    var shapeSection =
        sectionBetween(getSection, "Shape oficial da resposta `200 OK`:", "Response `200 OK`:");
    var responseBlock =
        sectionBetween(getSection, "Response `200 OK`:", "Response `404 Not Found`:");

    assertThat(shapeSection)
        .contains("- `slug`")
        .contains("- `name`")
        .contains("- `status`")
        .contains("- `phone`")
        .contains("- `hours`")
        .contains("- `deliveryZones`")
        .contains("- `dayOfWeek`")
        .contains("- `openTime`")
        .contains("- `closeTime`")
        .contains("- `closed`")
        .contains("- `zoneName`")
        .contains("- `feeAmount`")
        .contains("- `minOrderAmount`")
        .doesNotContain("logoUrl")
        .doesNotContain("bannerUrl")
        .doesNotContain("acceptsDelivery")
        .doesNotContain("acceptsPickup")
        .doesNotContain("id")
        .doesNotContain("cnpj")
        .doesNotContain("timezone")
        .doesNotContain("createdAt")
        .doesNotContain("hoursConfigured")
        .doesNotContain("deliveryZonesConfigured")
        .doesNotContain("active")
        .doesNotContain("storeId");

    assertThat(responseBlock)
        .contains("\"slug\": \"loja-do-bairro\"")
        .contains("\"name\": \"Loja do Bairro\"")
        .contains("\"status\": \"ACTIVE\"")
        .contains("\"phone\": \"21999990000\"")
        .contains("\"hours\": [")
        .contains("\"deliveryZones\": [")
        .contains("\"dayOfWeek\": \"MONDAY\"")
        .contains("\"openTime\": \"10:00:00\"")
        .contains("\"closeTime\": \"22:00:00\"")
        .contains("\"closed\": false")
        .contains("\"zoneName\": \"Centro\"")
        .contains("\"feeAmount\": 6.50")
        .contains("\"minOrderAmount\": 25.00")
        .doesNotContain("\"logoUrl\"")
        .doesNotContain("\"bannerUrl\"")
        .doesNotContain("\"acceptsDelivery\"")
        .doesNotContain("\"acceptsPickup\"")
        .doesNotContain("\"id\"")
        .doesNotContain("\"cnpj\"")
        .doesNotContain("\"timezone\"")
        .doesNotContain("\"createdAt\"")
        .doesNotContain("\"hoursConfigured\"")
        .doesNotContain("\"deliveryZonesConfigured\"")
        .doesNotContain("\"active\"")
        .doesNotContain("\"storeId\"");
  }

  private static String sectionBetween(String content, String startMarker, String endMarker) {
    var start = content.indexOf(startMarker);
    var end = content.indexOf(endMarker, start);
    assertThat(start).as("Missing marker: %s", startMarker).isGreaterThanOrEqualTo(0);
    assertThat(end).as("Missing marker: %s", endMarker).isGreaterThan(start);
    return content.substring(start, end);
  }
}
