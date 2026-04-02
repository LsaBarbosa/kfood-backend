package com.kfood.merchant.api;

import com.kfood.identity.app.Roles;
import com.kfood.merchant.app.AdminChangeStoreStatusUseCase;
import com.kfood.merchant.app.ChangeStoreStatusCommand;
import com.kfood.merchant.app.StoreDetailsOutput;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/stores")
public class AdminStoreStatusController {

  private final ObjectProvider<AdminChangeStoreStatusUseCase> adminChangeStoreStatusUseCaseProvider;

  public AdminStoreStatusController(
      ObjectProvider<AdminChangeStoreStatusUseCase> adminChangeStoreStatusUseCaseProvider) {
    this.adminChangeStoreStatusUseCaseProvider = adminChangeStoreStatusUseCaseProvider;
  }

  @PatchMapping("/{storeId}/status")
  @PreAuthorize(Roles.ADMIN)
  public StoreDetailsResponse changeStatus(
      @PathVariable UUID storeId, @Valid @RequestBody ChangeStoreStatusRequest request) {
    return toStoreDetailsResponse(
        adminChangeStoreStatusUseCase()
            .execute(storeId, new ChangeStoreStatusCommand(request.targetStatus())));
  }

  private AdminChangeStoreStatusUseCase adminChangeStoreStatusUseCase() {
    return adminChangeStoreStatusUseCaseProvider.getObject();
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
