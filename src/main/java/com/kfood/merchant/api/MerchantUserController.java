package com.kfood.merchant.api;

import com.kfood.identity.app.Roles;
import com.kfood.merchant.app.CreateMerchantUserUseCase;
import com.kfood.merchant.app.ListMerchantUsersUseCase;
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

  private final ObjectProvider<CreateMerchantUserUseCase> createMerchantUserUseCaseProvider;
  private final ObjectProvider<ListMerchantUsersUseCase> listMerchantUsersUseCaseProvider;

  public MerchantUserController(
      ObjectProvider<CreateMerchantUserUseCase> createMerchantUserUseCaseProvider,
      ObjectProvider<ListMerchantUsersUseCase> listMerchantUsersUseCaseProvider) {
    this.createMerchantUserUseCaseProvider = createMerchantUserUseCaseProvider;
    this.listMerchantUsersUseCaseProvider = listMerchantUsersUseCaseProvider;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public MerchantUserResponse create(@Valid @RequestBody CreateMerchantUserRequest request) {
    return createMerchantUserUseCaseProvider.getObject().execute(request);
  }

  @GetMapping
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public List<MerchantUserResponse> list() {
    return listMerchantUsersUseCaseProvider.getObject().execute();
  }
}
