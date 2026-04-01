package com.kfood.merchant.api;

import com.kfood.identity.app.Roles;
import com.kfood.merchant.app.ChangeStoreStatusCommand;
import com.kfood.merchant.app.ChangeStoreStatusUseCase;
import com.kfood.merchant.app.CreateStoreCommand;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceCommand;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceUseCase;
import com.kfood.merchant.app.CreateStoreUseCase;
import com.kfood.merchant.app.GetStoreDetailsUseCase;
import com.kfood.merchant.app.StoreDetailsOutput;
import com.kfood.merchant.app.StoreOutput;
import com.kfood.merchant.app.UpdateStoreCommand;
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
    var result =
        createStoreUseCase()
            .execute(
                new CreateStoreCommand(
                    request.name(),
                    request.slug(),
                    request.cnpj(),
                    request.phone(),
                    request.timezone()));
    return new CreateStoreResponse(result.id(), result.slug(), result.status(), result.createdAt());
  }

  @PutMapping
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public StoreResponse update(@Valid @RequestBody UpdateStoreRequest request) {
    return toStoreResponse(
        updateStoreUseCase()
            .execute(
                new UpdateStoreCommand(
                    request.name(),
                    request.slug(),
                    request.cnpj(),
                    request.phone(),
                    request.timezone())));
  }

  @PostMapping("/terms-acceptance")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Roles.OWNER)
  public StoreTermsAcceptanceResponse acceptTerms(
      @Valid @RequestBody CreateStoreTermsAcceptanceRequest request,
      HttpServletRequest httpServletRequest) {
    var result =
        createStoreTermsAcceptanceUseCase()
            .execute(
                new CreateStoreTermsAcceptanceCommand(
                    request.documentType(), request.documentVersion()),
                clientIpResolver.resolve(httpServletRequest));
    return new StoreTermsAcceptanceResponse(
        result.id(), result.documentType(), result.documentVersion(), result.acceptedAt());
  }

  @GetMapping
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public StoreDetailsResponse getCurrentStore() {
    return toStoreDetailsResponse(getStoreDetailsUseCase().execute());
  }

  @PatchMapping("/status")
  @PreAuthorize(Roles.OWNER_OR_ADMIN)
  public StoreDetailsResponse changeStatus(@Valid @RequestBody ChangeStoreStatusRequest request) {
    return toStoreDetailsResponse(
        changeStoreStatusUseCase().execute(new ChangeStoreStatusCommand(request.targetStatus())));
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

  private StoreResponse toStoreResponse(StoreOutput output) {
    return new StoreResponse(
        output.id(),
        output.name(),
        output.slug(),
        output.cnpj(),
        output.phone(),
        output.timezone(),
        output.status());
  }

  private StoreDetailsResponse toStoreDetailsResponse(StoreDetailsOutput output) {
    return new StoreDetailsResponse(
        output.id(),
        output.slug(),
        output.name(),
        output.status(),
        output.phone(),
        output.timezone(),
        output.hoursConfigured(),
        output.deliveryZonesConfigured());
  }
}
