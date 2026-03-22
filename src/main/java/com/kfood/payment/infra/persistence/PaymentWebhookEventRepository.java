package com.kfood.payment.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

  Optional<PaymentWebhookEvent> findByProviderNameAndExternalEventId(
      String providerName, String externalEventId);

  boolean existsByProviderNameAndExternalEventId(String providerName, String externalEventId);
}
