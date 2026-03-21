package com.kfood.customer.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  Optional<Customer> findByStoreIdAndPhone(UUID storeId, String phone);

  Optional<Customer> findByStoreIdAndEmail(UUID storeId, String email);
}
