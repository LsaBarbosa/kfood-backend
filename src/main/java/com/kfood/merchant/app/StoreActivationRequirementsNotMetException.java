package com.kfood.merchant.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.List;
import org.springframework.http.HttpStatus;

public class StoreActivationRequirementsNotMetException extends BusinessException {

  public StoreActivationRequirementsNotMetException(List<String> missingRequirements) {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Store cannot be activated. Missing requirements: "
            + String.join(", ", missingRequirements),
        HttpStatus.CONFLICT);
  }
}
