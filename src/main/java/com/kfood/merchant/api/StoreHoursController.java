package com.kfood.merchant.api;

import com.kfood.identity.app.Roles;
import com.kfood.merchant.app.GetStoreHoursUseCase;
import com.kfood.merchant.app.StoreHourCommand;
import com.kfood.merchant.app.StoreHourOutput;
import com.kfood.merchant.app.StoreHoursOutput;
import com.kfood.merchant.app.UpdateStoreHoursCommand;
import com.kfood.merchant.app.UpdateStoreHoursOutput;
import com.kfood.merchant.app.UpdateStoreHoursUseCase;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/merchant/store/hours")
public class StoreHoursController {

  private final ObjectProvider<UpdateStoreHoursUseCase> updateStoreHoursUseCaseProvider;
  private final ObjectProvider<GetStoreHoursUseCase> getStoreHoursUseCaseProvider;

  public StoreHoursController(
      ObjectProvider<UpdateStoreHoursUseCase> updateStoreHoursUseCaseProvider,
      ObjectProvider<GetStoreHoursUseCase> getStoreHoursUseCaseProvider) {
    this.updateStoreHoursUseCaseProvider = updateStoreHoursUseCaseProvider;
    this.getStoreHoursUseCaseProvider = getStoreHoursUseCaseProvider;
  }

  @PutMapping
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public UpdateStoreHoursResponse update(@Valid @RequestBody UpdateStoreHoursRequest request) {
    return toResponse(
        updateStoreHoursUseCase()
            .execute(
                new UpdateStoreHoursCommand(
                    request.hours().stream()
                        .map(
                            hour ->
                                new StoreHourCommand(
                                    hour.dayOfWeek(),
                                    hour.openTime(),
                                    hour.closeTime(),
                                    hour.closed()))
                        .toList())));
  }

  @GetMapping
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public StoreHoursResponse get() {
    return toResponse(getStoreHoursUseCase().execute());
  }

  private UpdateStoreHoursUseCase updateStoreHoursUseCase() {
    return updateStoreHoursUseCaseProvider.getObject();
  }

  private GetStoreHoursUseCase getStoreHoursUseCase() {
    return getStoreHoursUseCaseProvider.getObject();
  }

  private UpdateStoreHoursResponse toResponse(UpdateStoreHoursOutput output) {
    return new UpdateStoreHoursResponse(output.updated(), output.hoursVersion());
  }

  private StoreHoursResponse toResponse(StoreHoursOutput output) {
    return new StoreHoursResponse(
        output.hoursVersion(), output.hours().stream().map(this::toResponse).toList());
  }

  private StoreHourResponse toResponse(StoreHourOutput output) {
    return new StoreHourResponse(
        output.dayOfWeek(), output.openTime(), output.closeTime(), output.closed());
  }
}
