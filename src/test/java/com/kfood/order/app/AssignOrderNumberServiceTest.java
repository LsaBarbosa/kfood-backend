package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfood.order.app.port.OrderNumberTarget;
import org.junit.jupiter.api.Test;

class AssignOrderNumberServiceTest {

  @Test
  void shouldAssignNumberWhenMissing() {
    var generator = mock(OrderNumberGenerator.class);
    var service = new AssignOrderNumberService(generator);
    var order = mock(OrderNumberTarget.class);
    when(order.getOrderNumber()).thenReturn(null);
    when(generator.next(order)).thenReturn("PED-20260321-000001");

    service.assignIfMissing(order);

    verify(order).assignOrderNumber("PED-20260321-000001");
    verify(generator).next(order);
  }

  @Test
  void shouldNotReassignWhenAlreadyPresent() {
    var generator = mock(OrderNumberGenerator.class);
    var service = new AssignOrderNumberService(generator);
    var order = mock(OrderNumberTarget.class);
    when(order.getOrderNumber()).thenReturn("PED-20260321-000001");

    service.assignIfMissing(order);

    assertThat(order.getOrderNumber()).isEqualTo("PED-20260321-000001");
    verifyNoInteractions(generator);
  }
}
