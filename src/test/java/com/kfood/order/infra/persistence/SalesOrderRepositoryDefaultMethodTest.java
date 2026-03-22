package com.kfood.order.infra.persistence;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

class SalesOrderRepositoryDefaultMethodTest {

  @Test
  void shouldBuildSpecificationForOperationalQueueWithAllFilters() {
    var repository = mock(SalesOrderRepository.class, Mockito.CALLS_REAL_METHODS);
    var pageable = PageRequest.of(0, 20);
    when(repository.findAll(
            org.mockito.ArgumentMatchers.<Specification<SalesOrder>>any(), eq(pageable)))
        .thenReturn(Page.empty());

    repository.findOperationalQueue(
        UUID.randomUUID(),
        OrderStatus.NEW,
        FulfillmentType.DELIVERY,
        Instant.parse("2026-03-20T00:00:00Z"),
        Instant.parse("2026-03-21T00:00:00Z"),
        OffsetDateTime.parse("2026-03-20T15:00:00Z"),
        pageable);

    verify(repository)
        .findAll(org.mockito.ArgumentMatchers.<Specification<SalesOrder>>any(), eq(pageable));
  }

  @Test
  void shouldBuildSpecificationForOperationalQueueWithoutOptionalFilters() {
    var repository = mock(SalesOrderRepository.class, Mockito.CALLS_REAL_METHODS);
    var pageable = PageRequest.of(0, 20);
    when(repository.findAll(
            org.mockito.ArgumentMatchers.<Specification<SalesOrder>>any(), eq(pageable)))
        .thenReturn(Page.empty());

    repository.findOperationalQueue(
        UUID.randomUUID(),
        null,
        null,
        null,
        null,
        OffsetDateTime.parse("2026-03-20T15:00:00Z"),
        pageable);

    verify(repository)
        .findAll(org.mockito.ArgumentMatchers.<Specification<SalesOrder>>any(), eq(pageable));
  }
}
