package com.kfood.merchant.api;

import com.kfood.identity.app.Roles;
import com.kfood.merchant.app.ChangeStoreStatusUseCase;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceUseCase;
import com.kfood.merchant.app.CreateStoreUseCase;
import com.kfood.merchant.app.GetStoreDetailsUseCase;
import com.kfood.merchant.app.UpdateStoreUseCase;
import com.kfood.shared.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
  private final ObjectProvider<GetStoreDetailsUseCase> getStoreDetailsUseCaseProvider;
  private final ObjectProvider<CreateStoreTermsAcceptanceUseCase>
      createStoreTermsAcceptanceUseCaseProvider;
  private final ObjectProvider<ChangeStoreStatusUseCase> changeStoreStatusUseCaseProvider;
  private final ClientIpResolver clientIpResolver;

  public StoreController(
      ObjectProvider<CreateStoreUseCase> createStoreUseCaseProvider,
      ObjectProvider<UpdateStoreUseCase> updateStoreUseCaseProvider,
      ObjectProvider<GetStoreDetailsUseCase> getStoreDetailsUseCaseProvider,
      ObjectProvider<CreateStoreTermsAcceptanceUseCase> createStoreTermsAcceptanceUseCaseProvider,
      ObjectProvider<ChangeStoreStatusUseCase> changeStoreStatusUseCaseProvider,
      ClientIpResolver clientIpResolver) {
    this.createStoreUseCaseProvider = createStoreUseCaseProvider;
    this.updateStoreUseCaseProvider = updateStoreUseCaseProvider;
    this.getStoreDetailsUseCaseProvider = getStoreDetailsUseCaseProvider;
    this.createStoreTermsAcceptanceUseCaseProvider = createStoreTermsAcceptanceUseCaseProvider;
    this.changeStoreStatusUseCaseProvider = changeStoreStatusUseCaseProvider;
    this.clientIpResolver = clientIpResolver;
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

  @PostMapping("/terms-acceptance")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Roles.OWNER)
  public StoreTermsAcceptanceResponse acceptTerms(
      @Valid @RequestBody CreateStoreTermsAcceptanceRequest request,
      HttpServletRequest httpServletRequest) {
    return createStoreTermsAcceptanceUseCase()
        .execute(request, clientIpResolver.resolve(httpServletRequest));
  }

  @GetMapping
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public StoreDetailsResponse getCurrentStore() {
    return getStoreDetailsUseCase().execute();
  }

  @PatchMapping("/status")
  @PreAuthorize(Roles.OWNER_OR_ADMIN)
  public StoreDetailsResponse changeStatus(@Valid @RequestBody ChangeStoreStatusRequest request) {
    return changeStoreStatusUseCase().execute(request);
  }

  private CreateStoreUseCase createStoreUseCase() {
    return createStoreUseCaseProvider.getObject();
  }

  private UpdateStoreUseCase updateStoreUseCase() {
    return updateStoreUseCaseProvider.getObject();
  }

  private GetStoreDetailsUseCase getStoreDetailsUseCase() {
    return getStoreDetailsUseCaseProvider.getObject();
  }

  private CreateStoreTermsAcceptanceUseCase createStoreTermsAcceptanceUseCase() {
    return createStoreTermsAcceptanceUseCaseProvider.getObject();
  }

  private ChangeStoreStatusUseCase changeStoreStatusUseCase() {
    return changeStoreStatusUseCaseProvider.getObject();
  }
}
