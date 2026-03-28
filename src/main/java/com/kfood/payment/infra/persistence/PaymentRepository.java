package com.kfood.payment.infra.persistence;

import com.kfood.payment.app.port.PaymentPersistencePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, PaymentPersistencePort {

  @Override
  default Payment savePayment(Payment payment) {
    return save(payment);
  }

  @Override
  default Optional<Payment> findPaymentWithOrderByIdAndStoreId(UUID paymentId, UUID storeId) {
    return findDetailedByIdAndOrder_Store_Id(paymentId, storeId);
  }

  @EntityGraph(attributePaths = "order")
  Optional<Payment> findDetailedById(UUID id);

  @EntityGraph(attributePaths = "order")
  Optional<Payment> findDetailedByIdAndOrder_Store_Id(UUID id, UUID storeId);

  List<Payment> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);

  Optional<Payment> findByProviderNameAndProviderReference(
      String providerName, String providerReference);
}
