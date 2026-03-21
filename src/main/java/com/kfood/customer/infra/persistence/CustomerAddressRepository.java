package com.kfood.customer.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

  Optional<CustomerAddress> findByCustomerIdAndMainAddressTrue(UUID customerId);
}
