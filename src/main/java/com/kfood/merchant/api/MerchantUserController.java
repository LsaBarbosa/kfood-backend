package com.kfood.merchant.api;

import com.kfood.identity.app.Roles;
import com.kfood.merchant.application.user.CreateMerchantUserCommand;
import com.kfood.merchant.application.user.CreateMerchantUserUseCase;
import com.kfood.merchant.application.user.ListMerchantUsersUseCase;
import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/merchant/users")
public class MerchantUserController {

  private final ObjectProvider<MerchantUserManagementPort> merchantUserManagementPortProvider;
  private final ObjectProvider<MerchantTenantAccessPort> merchantTenantAccessPortProvider;

  public MerchantUserController(
      ObjectProvider<MerchantUserManagementPort> merchantUserManagementPortProvider,
      ObjectProvider<MerchantTenantAccessPort> merchantTenantAccessPortProvider) {
    this.merchantUserManagementPortProvider = merchantUserManagementPortProvider;
    this.merchantTenantAccessPortProvider = merchantTenantAccessPortProvider;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public MerchantUserResponse create(@Valid @RequestBody CreateMerchantUserRequest request) {
    return MerchantUserApiMapper.toResponse(
        createUseCase()
            .execute(
                new CreateMerchantUserCommand(
                    request.email(), request.password(), request.roles())));
  }

  @GetMapping
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public List<MerchantUserResponse> list() {
    return listUseCase().execute().stream().map(MerchantUserApiMapper::toResponse).toList();
  }

  private CreateMerchantUserUseCase createUseCase() {
    return new CreateMerchantUserUseCase(
        merchantUserManagementPortProvider.getObject(),
        merchantTenantAccessPortProvider.getObject());
  }

  private ListMerchantUsersUseCase listUseCase() {
    return new ListMerchantUsersUseCase(
        merchantUserManagementPortProvider.getObject(),
        merchantTenantAccessPortProvider.getObject());
  }
}
