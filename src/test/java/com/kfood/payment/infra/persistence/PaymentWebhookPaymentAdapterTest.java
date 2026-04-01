package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentWebhookPaymentAdapterTest {

  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final PaymentWebhookPaymentAdapter adapter =
      new PaymentWebhookPaymentAdapter(paymentRepository);

  @Test
  void shouldReturnCorrelatedPaymentWhenRepositoryFindsIt() {
    var payment = mock(Payment.class);
    when(paymentRepository.findDetailedByProviderNameAndProviderReference("mock", "charge-123"))
        .thenReturn(Optional.of(payment));

    var result = adapter.findByProviderNameAndProviderReference("mock", "charge-123");

    assertThat(result).containsSame(payment);
    verify(paymentRepository).findDetailedByProviderNameAndProviderReference("mock", "charge-123");
  }

  @Test
  void shouldReturnEmptyWhenRepositoryDoesNotFindPayment() {
    when(paymentRepository.findDetailedByProviderNameAndProviderReference("mock", "charge-123"))
        .thenReturn(Optional.empty());

    var result = adapter.findByProviderNameAndProviderReference("mock", "charge-123");

    assertThat(result).isEmpty();
    verify(paymentRepository).findDetailedByProviderNameAndProviderReference("mock", "charge-123");
  }
}
