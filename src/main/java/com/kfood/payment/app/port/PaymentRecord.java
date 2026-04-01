package com.kfood.payment.app.port;

import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface PaymentRecord {

  UUID getId();

  PaymentOrder getOrder();

  PaymentMethod getPaymentMethod();

  PaymentStatus getStatus();

  BigDecimal getAmount();

  String getProviderReference();

  String getQrCodePayload();

  Instant getCreatedAt();

  void attachPixChargeData(
      String providerName,
      String providerReference,
      String qrCodePayload,
      OffsetDateTime expiresAt);

  void changeStatus(PaymentStatus targetStatus, Instant currentTimestamp);
}
