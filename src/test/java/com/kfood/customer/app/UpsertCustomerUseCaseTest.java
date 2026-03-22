package com.kfood.customer.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.customer.api.CustomerAddressRequest;
import com.kfood.customer.api.UpsertCustomerRequest;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
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
  private final CustomerAddressRepository customerAddressRepository =
      mock(CustomerAddressRepository.class);
  private final UpsertCustomerUseCase upsertCustomerUseCase =
      new UpsertCustomerUseCase(storeRepository, customerRepository, customerAddressRepository);

  @Test
  void shouldCreateValidCustomer() {
    var store = store("loja-do-bairro");
    var request = new UpsertCustomerRequest(" Maria ", " 21999990000 ", " Maria@Email.com ", null);

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000"))
        .thenReturn(Optional.empty());
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.empty());
    when(customerRepository.saveAndFlush(any(Customer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(customerAddressRepository.findByCustomerIdAndMainAddressTrue(any(UUID.class)))
        .thenReturn(Optional.empty());

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
    var request = new UpsertCustomerRequest("Maria Silva", "21999990000", "maria@email.com", null);

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000"))
        .thenReturn(Optional.of(customer));
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.empty());
    when(customerRepository.saveAndFlush(customer)).thenReturn(customer);
    when(customerAddressRepository.findByCustomerIdAndMainAddressTrue(customer.getId()))
        .thenReturn(Optional.empty());

    var response = upsertCustomerUseCase.execute("loja-do-bairro", request);

    assertThat(response.id()).isEqualTo(customer.getId());
    assertThat(response.name()).isEqualTo("Maria Silva");
    assertThat(response.email()).isEqualTo("maria@email.com");
  }

  @Test
  void shouldUpdateExistingCustomerByEmail() {
    var store = store("loja-do-bairro");
    var customer = new Customer(UUID.randomUUID(), store, "Maria", null, "maria@email.com");
    var request = new UpsertCustomerRequest("Maria Silva", "21999990000", "maria@email.com", null);

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000"))
        .thenReturn(Optional.empty());
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.of(customer));
    when(customerRepository.saveAndFlush(customer)).thenReturn(customer);
    when(customerAddressRepository.findByCustomerIdAndMainAddressTrue(customer.getId()))
        .thenReturn(Optional.empty());

    var response = upsertCustomerUseCase.execute("loja-do-bairro", request);

    assertThat(response.id()).isEqualTo(customer.getId());
    assertThat(response.name()).isEqualTo("Maria Silva");
    assertThat(response.phone()).isEqualTo("21999990000");
  }

  @Test
  void shouldCreateCustomerUsingOnlyEmailWhenPhoneIsMissing() {
    var store = store("loja-do-bairro");
    var request = new UpsertCustomerRequest("Maria", null, "maria@email.com", null);

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.empty());
    when(customerRepository.saveAndFlush(any(Customer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(customerAddressRepository.findByCustomerIdAndMainAddressTrue(any(UUID.class)))
        .thenReturn(Optional.empty());

    var response = upsertCustomerUseCase.execute("loja-do-bairro", request);

    assertThat(response.phone()).isNull();
    assertThat(response.email()).isEqualTo("maria@email.com");
  }

  @Test
  void shouldCreateCustomerUsingOnlyPhoneWhenEmailIsMissing() {
    var store = store("loja-do-bairro");
    var request = new UpsertCustomerRequest("Maria", "21999990000", null, null);

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000"))
        .thenReturn(Optional.empty());
    when(customerRepository.saveAndFlush(any(Customer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(customerAddressRepository.findByCustomerIdAndMainAddressTrue(any(UUID.class)))
        .thenReturn(Optional.empty());

    var response = upsertCustomerUseCase.execute("loja-do-bairro", request);

    assertThat(response.phone()).isEqualTo("21999990000");
    assertThat(response.email()).isNull();
  }

  @Test
  void shouldRejectWhenPhoneAndEmailAreMissing() {
    when(storeRepository.findBySlug("loja-do-bairro"))
        .thenReturn(Optional.of(store("loja-do-bairro")));

    assertThatThrownBy(
            () ->
                upsertCustomerUseCase.execute(
                    "loja-do-bairro", new UpsertCustomerRequest("Maria", " ", null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("phone or email must be informed");

    verify(customerRepository, never()).saveAndFlush(any(Customer.class));
  }

  @Test
  void shouldRejectWhenIdentifiersBelongToDifferentCustomers() {
    var store = store("loja-do-bairro");
    var phoneCustomer = new Customer(UUID.randomUUID(), store, "Maria", "21999990000", null);
    var emailCustomer = new Customer(UUID.randomUUID(), store, "Joao", null, "maria@email.com");
    var request = new UpsertCustomerRequest("Maria", "21999990000", "maria@email.com", null);

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
                    new UpsertCustomerRequest("Maria", "21999990000", "maria@email.com", null)))
        .isInstanceOf(StoreSlugNotFoundException.class)
        .hasMessageContaining("loja-inexistente");
  }

  @Test
  void shouldRejectNullStoreSlug() {
    assertThatThrownBy(
            () ->
                upsertCustomerUseCase.execute(
                    null,
                    new UpsertCustomerRequest("Maria", "21999990000", "maria@email.com", null)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("storeSlug is required");
  }

  @Test
  void shouldTrimBlankStoreSlugBeforeLookup() {
    when(storeRepository.findBySlug("")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                upsertCustomerUseCase.execute(
                    "   ",
                    new UpsertCustomerRequest("Maria", "21999990000", "maria@email.com", null)))
        .isInstanceOf(StoreSlugNotFoundException.class);

    verify(storeRepository).findBySlug("");
  }

  @Test
  void shouldSaveMainAddressWhenProvided() {
    var store = store("loja-do-bairro");
    var request =
        new UpsertCustomerRequest(
            "Maria Silva",
            "21999990000",
            "maria@email.com",
            new CustomerAddressRequest(
                "Casa",
                "25000-000",
                "Rua das Flores",
                "45",
                "Centro",
                "Mage",
                "rj",
                "Ap 101",
                true));

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000"))
        .thenReturn(Optional.empty());
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.empty());
    when(customerRepository.saveAndFlush(any(Customer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(customerAddressRepository.findByCustomerIdAndMainAddressTrue(any(UUID.class)))
        .thenReturn(Optional.empty());
    when(customerAddressRepository.saveAndFlush(any(CustomerAddress.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = upsertCustomerUseCase.execute("loja-do-bairro", request);

    assertThat(response.mainAddressId()).isNotNull();
  }

  @Test
  void shouldKeepExistingMainAddressWhenSavingSecondaryAddress() {
    var store = store("loja-do-bairro");
    var customer =
        new Customer(UUID.randomUUID(), store, "Maria", "21999990000", "maria@email.com");
    var currentMainAddress =
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
            true);
    var request =
        new UpsertCustomerRequest(
            "Maria",
            "21999990000",
            "maria@email.com",
            new CustomerAddressRequest(
                "Trabalho",
                "22000-000",
                "Avenida Central",
                "100",
                "Centro",
                "Rio de Janeiro",
                "RJ",
                null,
                false));

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndPhone(store.getId(), "21999990000"))
        .thenReturn(Optional.of(customer));
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.of(customer));
    when(customerRepository.saveAndFlush(customer)).thenReturn(customer);
    when(customerAddressRepository.saveAndFlush(any(CustomerAddress.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(customerAddressRepository.findByCustomerIdAndMainAddressTrue(customer.getId()))
        .thenReturn(Optional.of(currentMainAddress));

    var response = upsertCustomerUseCase.execute("loja-do-bairro", request);

    assertThat(response.mainAddressId()).isEqualTo(currentMainAddress.getId());
  }

  @Test
  void shouldUnsetExistingMainAddressWhenSavingNewMainAddress() {
    var store = store("loja-do-bairro");
    var customer = new Customer(UUID.randomUUID(), store, "Maria", null, "maria@email.com");
    var existingMainAddress =
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
            true);
    var request =
        new UpsertCustomerRequest(
            "Maria",
            null,
            "maria@email.com",
            new CustomerAddressRequest(
                "Trabalho",
                "22000-000",
                "Avenida Central",
                "100",
                "Centro",
                "Rio de Janeiro",
                "RJ",
                null,
                true));

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(customerRepository.findByStoreIdAndEmail(store.getId(), "maria@email.com"))
        .thenReturn(Optional.of(customer));
    when(customerRepository.saveAndFlush(customer)).thenReturn(customer);
    when(customerAddressRepository.findByCustomerIdAndMainAddressTrue(customer.getId()))
        .thenReturn(Optional.of(existingMainAddress));
    when(customerAddressRepository.saveAndFlush(any(CustomerAddress.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = upsertCustomerUseCase.execute("loja-do-bairro", request);

    assertThat(response.mainAddressId()).isNotNull();
    verify(customerAddressRepository).save(existingMainAddress);
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
