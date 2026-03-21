package com.kfood.merchant.app;

import com.kfood.shared.exceptions.ApiFieldError;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.List;
import org.springframework.http.HttpStatus;

public class StoreActivationRequirementsNotMetException extends BusinessException {

  public StoreActivationRequirementsNotMetException(List<String> missingRequirements) {
    super(
        ErrorCode.STORE_NOT_ACTIVE,
        "Store is not ready to be activated. Missing requirements: "
            + String.join(", ", missingRequirements),
        HttpStatus.CONFLICT,
        missingRequirements.stream()
            .map(requirement -> new ApiFieldError(requirement, "Required for store activation."))
            .toList());
  }
}
