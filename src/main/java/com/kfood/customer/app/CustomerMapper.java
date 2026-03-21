package com.kfood.customer.app;

import com.kfood.customer.api.CustomerResponse;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;

public final class CustomerMapper {

  private CustomerMapper() {}

  public static CustomerResponse toResponse(Customer customer, CustomerAddress mainAddress) {
    return new CustomerResponse(
        customer.getId(),
        customer.getName(),
        customer.getPhone(),
        customer.getEmail(),
        mainAddress == null ? null : mainAddress.getId());
  }
}
