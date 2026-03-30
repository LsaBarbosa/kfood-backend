package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import org.junit.jupiter.api.Test;

class PaymentStatusSnapshotMapperTest {

  @Test
  void shouldMapPendingToPendingSnapshot() {
    assertThat(PaymentStatusSnapshotMapper.from(PaymentStatus.PENDING))
        .isEqualTo(PaymentStatusSnapshot.PENDING);
  }

  @Test
  void shouldMapConfirmedToPaidSnapshot() {
    assertThat(PaymentStatusSnapshotMapper.from(PaymentStatus.CONFIRMED))
        .isEqualTo(PaymentStatusSnapshot.PAID);
  }

  @Test
  void shouldMapFailedCanceledAndExpiredToFailedSnapshot() {
    assertThat(PaymentStatusSnapshotMapper.from(PaymentStatus.FAILED))
        .isEqualTo(PaymentStatusSnapshot.FAILED);
    assertThat(PaymentStatusSnapshotMapper.from(PaymentStatus.CANCELED))
        .isEqualTo(PaymentStatusSnapshot.FAILED);
    assertThat(PaymentStatusSnapshotMapper.from(PaymentStatus.EXPIRED))
        .isEqualTo(PaymentStatusSnapshot.FAILED);
  }

  @Test
  void shouldRejectNullPaymentStatus() {
    assertThatThrownBy(() -> PaymentStatusSnapshotMapper.from(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("paymentStatus must not be null");
  }
}
