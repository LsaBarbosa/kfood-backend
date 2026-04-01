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
class CustomerAddressRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private StoreRepository storeRepository;

  @Autowired private CustomerRepository customerRepository;

  @Autowired private CustomerAddressRepository customerAddressRepository;

  @Test
  @DisplayName("should find main address by customer id")
  void shouldFindMainAddressByCustomerId() {
    var store = storeRepository.saveAndFlush(store("loja-do-bairro", "45.723.174/0001-10"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com"));
    var address =
        customerAddressRepository.saveAndFlush(
            new CustomerAddress(
                UUID.randomUUID(),
                customer,
                "Casa",
                "25000000",
                "Rua das Flores",
                "45",
                "Centro",
                "Mage",
                "RJ",
                null,
                true));

    var savedAddress =
        customerAddressRepository.findByCustomerIdAndMainAddressTrue(customer.getId());

    assertThat(savedAddress).isPresent();
    assertThat(savedAddress.get().getId()).isEqualTo(address.getId());
    assertThat(savedAddress.get().getCreatedAt()).isNotNull();
    assertThat(savedAddress.get().getUpdatedAt()).isNotNull();
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
