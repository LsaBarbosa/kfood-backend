package com.kfood.customer.app;

import com.kfood.customer.api.CustomerResponse;
import com.kfood.customer.infra.persistence.Customer;

public final class CustomerMapper {

  private CustomerMapper() {}

  public static CustomerResponse toResponse(Customer customer) {
    return new CustomerResponse(
        customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail());
  }
}
