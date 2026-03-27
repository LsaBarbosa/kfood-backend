package com.kfood.catalog.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogOptionGroupLookupAdapterTest {

  private final CatalogOptionGroupRepository catalogOptionGroupRepository =
      mock(CatalogOptionGroupRepository.class);
  private final CatalogOptionGroupLookupAdapter adapter =
      new CatalogOptionGroupLookupAdapter(catalogOptionGroupRepository);

  @Test
  void shouldDelegateLookupToRepository() {
    var productId = UUID.randomUUID();
    var expected = List.of(mock(CatalogOptionGroup.class));
    when(catalogOptionGroupRepository.findAllByProduct_IdAndActiveTrueOrderByIdAsc(productId))
        .thenReturn(expected);

    var result = adapter.findActiveByProductId(productId);

    assertThat(result).isSameAs(expected);
  }
}
