package com.kfood.merchant.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.stream.Stream;
import org.hibernate.validator.constraints.br.CNPJ;

public record UpdateStoreRequest(
    @Pattern(regexp = "^(?=.*\\S).+$", message = "name must not be blank") @Size(max = 160) String name,
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "slug must contain only lowercase letters, numbers and hyphens")
        @Size(max = 120) String slug,
    @CNPJ String cnpj,
    @Pattern(regexp = "^\\d{10,15}$", message = "phone must contain between 10 and 15 digits") String phone,
    @Pattern(regexp = "^(?=.*\\S).+$", message = "timezone must not be blank") @Size(max = 60) String timezone) {

  @AssertTrue(message = "at least one field must be informed") public boolean hasAnyFieldInformed() {
    return Stream.of(name, slug, cnpj, phone, timezone).anyMatch(Objects::nonNull);
  }
}
