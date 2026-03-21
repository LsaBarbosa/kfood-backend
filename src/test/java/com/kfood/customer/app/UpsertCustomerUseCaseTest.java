package com.kfood.customer.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.customer.api.UpsertCustomerRequest;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.exceptions.BusinessException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpsertCustomerUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CustomerRepository customerRepository = mock(CustomerRepository.class);
  private final UpsertCustomerUseCase upsertCustomerUseCase =
      new UpsertCustomerUseCase(storeRepository, customerRepository);

  @Test
  void shouldCreateValidCustomer() {
    var store = store("loja-do-bairro");
    var request = new UpsertCustomerRequest(" Maria ", " 21999990000 ", " Maria@Email.com ");

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000"))
        .thenReturn(Optional.empty());
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.empty());
    when(customerRepository.saveAndFlush(any(Customer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = upsertCustomerUseCase.execute(" loja-do-bairro ", request);

    assertThat(response.name()).isEqualTo("Maria");
    assertThat(response.phone()).isEqualTo("21999990000");
    assertThat(response.email()).isEqualTo("maria@email.com");
  }

  @Test
  void shouldUpdateExistingCustomerByPhone() {
    var store = store("loja-do-bairro");
    var customer =
        new Customer(UUID.randomUUID(), store, "Maria", "21999990000", "maria.antiga@email.com");
    var request = new UpsertCustomerRequest("Maria Silva", "21999990000", "maria@email.com");

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000"))
        .thenReturn(Optional.of(customer));
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.empty());
    when(customerRepository.saveAndFlush(customer)).thenReturn(customer);

    var response = upsertCustomerUseCase.execute("loja-do-bairro", request);

    assertThat(response.id()).isEqualTo(customer.getId());
    assertThat(response.name()).isEqualTo("Maria Silva");
    assertThat(response.email()).isEqualTo("maria@email.com");
  }

  @Test
  void shouldUpdateExistingCustomerByEmail() {
    var store = store("loja-do-bairro");
    var customer = new Customer(UUID.randomUUID(), store, "Maria", null, "maria@email.com");
    var request = new UpsertCustomerRequest("Maria Silva", "21999990000", "maria@email.com");

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000"))
        .thenReturn(Optional.empty());
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.of(customer));
    when(customerRepository.saveAndFlush(customer)).thenReturn(customer);

    var response = upsertCustomerUseCase.execute("loja-do-bairro", request);

    assertThat(response.id()).isEqualTo(customer.getId());
    assertThat(response.name()).isEqualTo("Maria Silva");
    assertThat(response.phone()).isEqualTo("21999990000");
  }

  @Test
  void shouldRejectWhenPhoneAndEmailAreMissing() {
    when(storeRepository.findBySlug("loja-do-bairro"))
        .thenReturn(Optional.of(store("loja-do-bairro")));

    assertThatThrownBy(
            () ->
                upsertCustomerUseCase.execute(
                    "loja-do-bairro", new UpsertCustomerRequest("Maria", " ", null)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("phone or email must be informed");

    verify(customerRepository, never()).saveAndFlush(any(Customer.class));
  }

  @Test
  void shouldRejectWhenIdentifiersBelongToDifferentCustomers() {
    var store = store("loja-do-bairro");
    var phoneCustomer = new Customer(UUID.randomUUID(), store, "Maria", "21999990000", null);
    var emailCustomer = new Customer(UUID.randomUUID(), store, "Joao", null, "maria@email.com");
    var request = new UpsertCustomerRequest("Maria", "21999990000", "maria@email.com");

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000"))
        .thenReturn(Optional.of(phoneCustomer));
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.of(emailCustomer));

    assertThatThrownBy(() -> upsertCustomerUseCase.execute("loja-do-bairro", request))
        .isInstanceOf(CustomerIdentifierConflictException.class)
        .hasMessage("Phone and email belong to different customers.");
  }

  @Test
  void shouldThrowWhenStoreSlugDoesNotExist() {
    when(storeRepository.findBySlug("loja-inexistente")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                upsertCustomerUseCase.execute(
                    "loja-inexistente",
                    new UpsertCustomerRequest("Maria", "21999990000", "maria@email.com")))
        .isInstanceOf(StoreSlugNotFoundException.class)
        .hasMessageContaining("loja-inexistente");
  }

  private Store store(String slug) {
    return new Store(
        UUID.randomUUID(),
        "Loja do Bairro",
        slug,
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }
}
