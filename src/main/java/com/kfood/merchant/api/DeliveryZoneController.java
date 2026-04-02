package com.kfood.merchant.api;

import com.kfood.identity.app.Roles;
import com.kfood.merchant.app.CreateDeliveryZoneCommand;
import com.kfood.merchant.app.CreateDeliveryZoneUseCase;
import com.kfood.merchant.app.DeleteDeliveryZoneUseCase;
import com.kfood.merchant.app.DeliveryZoneOutput;
import com.kfood.merchant.app.GetDeliveryZoneUseCase;
import com.kfood.merchant.app.ListDeliveryZonesUseCase;
import com.kfood.merchant.app.UpdateDeliveryZoneCommand;
import com.kfood.merchant.app.UpdateDeliveryZoneUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/merchant/store/zones")
public class DeliveryZoneController {

  private final ObjectProvider<CreateDeliveryZoneUseCase> createDeliveryZoneUseCaseProvider;
  private final ObjectProvider<GetDeliveryZoneUseCase> getDeliveryZoneUseCaseProvider;
  private final ObjectProvider<ListDeliveryZonesUseCase> listDeliveryZonesUseCaseProvider;
  private final ObjectProvider<UpdateDeliveryZoneUseCase> updateDeliveryZoneUseCaseProvider;
  private final ObjectProvider<DeleteDeliveryZoneUseCase> deleteDeliveryZoneUseCaseProvider;

  public DeliveryZoneController(
      ObjectProvider<CreateDeliveryZoneUseCase> createDeliveryZoneUseCaseProvider,
      ObjectProvider<GetDeliveryZoneUseCase> getDeliveryZoneUseCaseProvider,
      ObjectProvider<ListDeliveryZonesUseCase> listDeliveryZonesUseCaseProvider,
      ObjectProvider<UpdateDeliveryZoneUseCase> updateDeliveryZoneUseCaseProvider,
      ObjectProvider<DeleteDeliveryZoneUseCase> deleteDeliveryZoneUseCaseProvider) {
    this.createDeliveryZoneUseCaseProvider = createDeliveryZoneUseCaseProvider;
    this.getDeliveryZoneUseCaseProvider = getDeliveryZoneUseCaseProvider;
    this.listDeliveryZonesUseCaseProvider = listDeliveryZonesUseCaseProvider;
    this.updateDeliveryZoneUseCaseProvider = updateDeliveryZoneUseCaseProvider;
    this.deleteDeliveryZoneUseCaseProvider = deleteDeliveryZoneUseCaseProvider;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public DeliveryZoneResponse create(@Valid @RequestBody CreateDeliveryZoneRequest request) {
    return toResponse(
        createDeliveryZoneUseCase()
            .execute(
                new CreateDeliveryZoneCommand(
                    request.zoneName(),
                    request.feeAmount(),
                    request.minOrderAmount(),
                    request.active())));
  }

  @GetMapping("/{zoneId}")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public DeliveryZoneResponse getById(@PathVariable UUID zoneId) {
    return toResponse(getDeliveryZoneUseCase().execute(zoneId));
  }

  @GetMapping
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public List<DeliveryZoneResponse> list() {
    return listDeliveryZonesUseCase().execute().stream().map(this::toResponse).toList();
  }

  @PutMapping("/{zoneId}")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public DeliveryZoneResponse update(
      @PathVariable UUID zoneId, @Valid @RequestBody CreateDeliveryZoneRequest request) {
    return toResponse(
        updateDeliveryZoneUseCase()
            .execute(
                zoneId,
                new UpdateDeliveryZoneCommand(
                    request.zoneName(),
                    request.feeAmount(),
                    request.minOrderAmount(),
                    request.active())));
  }

  @DeleteMapping("/{zoneId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public void delete(@PathVariable UUID zoneId) {
    deleteDeliveryZoneUseCase().execute(zoneId);
  }

  private CreateDeliveryZoneUseCase createDeliveryZoneUseCase() {
    return createDeliveryZoneUseCaseProvider.getObject();
  }

  private GetDeliveryZoneUseCase getDeliveryZoneUseCase() {
    return getDeliveryZoneUseCaseProvider.getObject();
  }

  private ListDeliveryZonesUseCase listDeliveryZonesUseCase() {
    return listDeliveryZonesUseCaseProvider.getObject();
  }

  private UpdateDeliveryZoneUseCase updateDeliveryZoneUseCase() {
    return updateDeliveryZoneUseCaseProvider.getObject();
  }

  private DeleteDeliveryZoneUseCase deleteDeliveryZoneUseCase() {
    return deleteDeliveryZoneUseCaseProvider.getObject();
  }

  private DeliveryZoneResponse toResponse(DeliveryZoneOutput output) {
    return new DeliveryZoneResponse(
        output.id(),
        output.zoneName(),
        output.feeAmount(),
        output.minOrderAmount(),
        output.active());
  }
}
