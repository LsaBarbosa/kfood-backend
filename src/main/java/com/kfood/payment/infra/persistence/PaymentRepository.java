package com.kfood.payment.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  List<Payment> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);

  Optional<Payment> findByProviderNameAndProviderReference(
      String providerName, String providerReference);
}
