package com.kfood.customer.app;

import com.kfood.customer.api.CustomerResponse;
import com.kfood.customer.api.UpsertCustomerRequest;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({StoreRepository.class, CustomerRepository.class})
public class UpsertCustomerUseCase {

  private final StoreRepository storeRepository;
  private final CustomerRepository customerRepository;

  public UpsertCustomerUseCase(
      StoreRepository storeRepository, CustomerRepository customerRepository) {
    this.storeRepository = storeRepository;
    this.customerRepository = customerRepository;
  }

  @Transactional
  public CustomerResponse execute(String storeSlug, UpsertCustomerRequest request) {
    var normalizedSlug = normalizeRequired(storeSlug, "storeSlug is required");
    var normalizedPhone = normalizeNullable(request.phone());
    var normalizedEmail = normalizeEmail(request.email());

    if (normalizedPhone == null && normalizedEmail == null) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR, "phone or email must be informed", HttpStatus.BAD_REQUEST);
    }

    var store =
        storeRepository
            .findBySlug(normalizedSlug)
            .orElseThrow(() -> new StoreSlugNotFoundException(normalizedSlug));

    var phoneCustomer =
        normalizedPhone == null
            ? Optional.<Customer>empty()
            : customerRepository.findByStoreIdAndPhone(store.getId(), normalizedPhone);
    var emailCustomer =
        normalizedEmail == null
            ? Optional.<Customer>empty()
            : customerRepository.findByStoreIdAndEmail(store.getId(), normalizedEmail);

    if (phoneCustomer.isPresent()
        && emailCustomer.isPresent()
        && !phoneCustomer.get().getId().equals(emailCustomer.get().getId())) {
      throw new CustomerIdentifierConflictException();
    }

    var customer =
        phoneCustomer
            .or(() -> emailCustomer)
            .orElseGet(
                () ->
                    new Customer(
                        UUID.randomUUID(),
                        store,
                        request.name(),
                        normalizedPhone,
                        normalizedEmail));

    customer.update(request.name(), normalizedPhone, normalizedEmail);

    return CustomerMapper.toResponse(customerRepository.saveAndFlush(customer));
  }

  private String normalizeRequired(String value, String message) {
    return normalize(Objects.requireNonNull(value, message));
  }

  private String normalizeEmail(String value) {
    var normalized = normalizeNullable(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  private String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : normalize(value);
  }

  private String normalize(String value) {
    return value.trim();
  }
}
