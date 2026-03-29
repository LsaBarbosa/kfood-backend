package com.kfood.merchant.app.port;

import com.kfood.merchant.app.DeliveryZoneOutput;
import com.kfood.merchant.app.PublicStoreMenuOutput;
import com.kfood.merchant.app.PublicStoreOutput;
import com.kfood.merchant.app.StoreActivationRequirements;
import com.kfood.merchant.app.StoreDetailsOutput;
import com.kfood.merchant.app.StoreHoursOutput;
import com.kfood.merchant.app.StoreTermsAcceptanceHistoryItemOutput;
import java.util.List;
import java.util.UUID;

public interface MerchantQueryPort {

  StoreDetailsOutput getStoreDetails(UUID storeId, StoreActivationRequirements requirements);

  DeliveryZoneOutput getDeliveryZone(UUID storeId, UUID zoneId);

  List<DeliveryZoneOutput> listDeliveryZones(UUID storeId);

  StoreHoursOutput getStoreHours(UUID storeId);

  PublicStoreOutput getPublicStore(String slug);

  PublicStoreMenuOutput getPublicStoreMenu(String slug);

  List<StoreTermsAcceptanceHistoryItemOutput> getStoreTermsAcceptanceHistory(UUID storeId);
}
