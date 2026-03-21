package com.kfood.customer.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class CustomerIdentifierConflictException extends BusinessException {

  public CustomerIdentifierConflictException() {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Phone and email belong to different customers.",
        HttpStatus.CONFLICT);
  }
}
