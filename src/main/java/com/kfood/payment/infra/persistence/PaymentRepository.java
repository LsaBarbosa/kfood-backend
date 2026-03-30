package com.kfood.payment.infra.persistence;

import com.kfood.payment.app.port.PaymentOrder;
import com.kfood.payment.app.port.PaymentPersistencePort;
import com.kfood.payment.app.port.PaymentRecord;
import com.kfood.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, PaymentPersistencePort {

  @Override
  default PaymentRecord savePendingPayment(
      UUID paymentId, PaymentOrder order, PaymentMethod paymentMethod, BigDecimal amount) {
    return save(
        Payment.createPending(
            paymentId,
            (com.kfood.order.infra.persistence.SalesOrder) order,
            paymentMethod,
            amount));
  }

  @Override
  default Optional<PaymentRecord> findPaymentWithOrderByIdAndStoreId(UUID paymentId, UUID storeId) {
    return findDetailedByIdAndOrder_Store_Id(paymentId, storeId).map(payment -> payment);
  }

  @EntityGraph(attributePaths = "order")
  Optional<Payment> findDetailedById(UUID id);

  @EntityGraph(attributePaths = "order")
  Optional<Payment> findDetailedByIdAndOrder_Store_Id(UUID id, UUID storeId);

  List<Payment> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);

  Optional<Payment> findByProviderNameAndProviderReference(
      String providerName, String providerReference);
}
