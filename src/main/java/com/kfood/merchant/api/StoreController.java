package com.kfood.merchant.api;

import com.kfood.identity.app.Roles;
import com.kfood.merchant.app.CreateStoreUseCase;
import com.kfood.merchant.app.UpdateStoreUseCase;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/merchant/store")
public class StoreController {

  private final ObjectProvider<CreateStoreUseCase> createStoreUseCaseProvider;
  private final ObjectProvider<UpdateStoreUseCase> updateStoreUseCaseProvider;

  public StoreController(
      ObjectProvider<CreateStoreUseCase> createStoreUseCaseProvider,
      ObjectProvider<UpdateStoreUseCase> updateStoreUseCaseProvider) {
    this.createStoreUseCaseProvider = createStoreUseCaseProvider;
    this.updateStoreUseCaseProvider = updateStoreUseCaseProvider;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Roles.OWNER_OR_ADMIN)
  public CreateStoreResponse create(@Valid @RequestBody CreateStoreRequest request) {
    return createStoreUseCase().execute(request);
  }

  @PutMapping
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public StoreResponse update(@Valid @RequestBody UpdateStoreRequest request) {
    return updateStoreUseCase().execute(request);
  }

  private CreateStoreUseCase createStoreUseCase() {
    return createStoreUseCaseProvider.getObject();
  }

  private UpdateStoreUseCase updateStoreUseCase() {
    return updateStoreUseCaseProvider.getObject();
  }
}
