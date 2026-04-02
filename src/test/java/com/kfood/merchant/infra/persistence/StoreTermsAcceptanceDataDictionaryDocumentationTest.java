package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StoreTermsAcceptanceDataDictionaryDocumentationTest {

  private static final Path DATA_DICTIONARY_PATH =
      Path.of("docs/data/store_terms_acceptance_data_dictionary.md");

  @Test
  void shouldPreventStoreTermsAcceptanceDataDictionaryFromOmittingRequestIpColumn()
      throws IOException {
    var dataDictionary = Files.readString(DATA_DICTIONARY_PATH, StandardCharsets.UTF_8);
    var columnsSection = sectionBetween(dataDictionary, "## Columns", "## Notes");

    assertThat(dataDictionary).contains("- name: `store_terms_acceptance`");
    assertThat(columnsSection).contains("| `request_ip` | `varchar(45)` | yes |");
    assertThat(dataDictionary)
        .contains("`request_ip` is derived and normalized by the backend from the HTTP request metadata before persistence.");
  }

  private static String sectionBetween(String content, String startMarker, String endMarker) {
    var start = content.indexOf(startMarker);
    var end = content.indexOf(endMarker, start);
    assertThat(start).as("Missing marker: %s", startMarker).isGreaterThanOrEqualTo(0);
    assertThat(end).as("Missing marker: %s", endMarker).isGreaterThan(start);
    return content.substring(start, end);
  }
}
