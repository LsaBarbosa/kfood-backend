package com.kfood.customer.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class CustomerRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private StoreRepository storeRepository;

  @Autowired private CustomerRepository customerRepository;

  @Test
  @DisplayName("should persist valid customer")
  void shouldPersistValidCustomer() {
    var store = storeRepository.saveAndFlush(store("loja-do-bairro", "45.723.174/0001-10"));
    var customer =
        new Customer(UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com");

    var savedCustomer = customerRepository.saveAndFlush(customer);

    assertThat(savedCustomer.getId()).isNotNull();
    assertThat(savedCustomer.getCreatedAt()).isNotNull();
    assertThat(savedCustomer.getUpdatedAt()).isNotNull();
    assertThat(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000")).isPresent();
    assertThat(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .isPresent();
  }

  @Test
  @DisplayName("should allow same phone in different stores")
  void shouldAllowSamePhoneInDifferentStores() {
    var firstStore = storeRepository.saveAndFlush(store("loja-a", "45.723.174/0001-10"));
    var secondStore = storeRepository.saveAndFlush(store("loja-b", "54.550.752/0001-55"));

    customerRepository.saveAndFlush(
        new Customer(UUID.randomUUID(), firstStore, "Maria", "21999990000", null));
    customerRepository.saveAndFlush(
        new Customer(UUID.randomUUID(), secondStore, "Joao", "21999990000", null));

    assertThat(customerRepository.findByStoreIdAndPhone(firstStore.getId(), "21999990000"))
        .isPresent();
    assertThat(customerRepository.findByStoreIdAndPhone(secondStore.getId(), "21999990000"))
        .isPresent();
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
