package com.kfood.merchant.app.port;

import com.kfood.merchant.app.ChangeStoreStatusCommand;
import com.kfood.merchant.app.CreateDeliveryZoneCommand;
import com.kfood.merchant.app.CreateStoreCommand;
import com.kfood.merchant.app.CreateStoreOutput;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceCommand;
import com.kfood.merchant.app.DeliveryZoneOutput;
import com.kfood.merchant.app.StoreActivationRequirements;
import com.kfood.merchant.app.StoreDetailsOutput;
import com.kfood.merchant.app.StoreOutput;
import com.kfood.merchant.app.StoreTermsAcceptanceOutput;
import com.kfood.merchant.app.UpdateDeliveryZoneCommand;
import com.kfood.merchant.app.UpdateStoreCommand;
import com.kfood.merchant.app.UpdateStoreHoursCommand;
import com.kfood.merchant.app.UpdateStoreHoursOutput;
import java.time.Instant;
import java.util.UUID;

public interface MerchantCommandPort {

  DeliveryZoneOutput createDeliveryZone(UUID storeId, CreateDeliveryZoneCommand command);

  DeliveryZoneOutput updateDeliveryZone(
      UUID storeId, UUID zoneId, UpdateDeliveryZoneCommand command);

  void deleteDeliveryZone(UUID storeId, UUID zoneId);

  UpdateStoreHoursOutput updateStoreHours(UUID storeId, UpdateStoreHoursCommand command);

  StoreOutput updateStore(UUID storeId, UpdateStoreCommand command);

  CreateStoreOutput createStore(UUID authenticatedUserId, CreateStoreCommand command);

  StoreDetailsOutput changeStoreStatus(
      UUID storeId, ChangeStoreStatusCommand command, StoreActivationRequirements requirements);

  StoreTermsAcceptanceOutput createStoreTermsAcceptance(
      UUID storeId,
      UUID authenticatedUserId,
      CreateStoreTermsAcceptanceCommand command,
      String requestIp,
      Instant acceptedAt);
}
