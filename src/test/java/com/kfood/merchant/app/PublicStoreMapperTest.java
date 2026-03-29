package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.domain.StoreStatus;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicStoreMapperTest {

  @Test
  void shouldMapPublicStoreBoundaryObjects() {
    var store = store();
    var hour = new MerchantViews.StoreHourView(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false);
    var zone =
        new MerchantViews.DeliveryZoneView(
            UUID.randomUUID(), "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true);

    var mappedHour = PublicStoreMapper.toHourOutput(hour);
    var mappedZone = PublicStoreMapper.toDeliveryZoneOutput(zone);
    var output = PublicStoreMapper.toOutput(store, List.of(mappedHour), List.of(mappedZone));

    assertThat(mappedHour.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(mappedHour.openTime()).isEqualTo(LocalTime.of(10, 0));
    assertThat(mappedZone.zoneName()).isEqualTo("Centro");
    assertThat(output.slug()).isEqualTo("loja-do-bairro");
    assertThat(output.status()).isEqualTo(StoreStatus.ACTIVE);
    assertThat(output.deliveryZones()).containsExactly(mappedZone);
  }

  @Test
  void shouldMapOptionGroupAndItem() {
    var product =
        new MerchantViews.PublicStoreMenuProductView(
            UUID.randomUUID(),
            "Pizza Calabresa",
            "Descricao",
            new BigDecimal("39.90"),
            null,
            false,
            List.of(
                new MerchantViews.PublicStoreMenuOptionGroupView(
                    UUID.randomUUID(),
                    "Bordas",
                    1,
                    2,
                    true,
                    List.of(
                        new MerchantViews.PublicStoreMenuOptionItemView(
                            UUID.randomUUID(), "Catupiry", new BigDecimal("8.00"), 10)))));

    var response = PublicStoreMapper.toMenuProductOutput(product);

    assertThat(response.optionGroups()).hasSize(1);
    var mappedGroup = response.optionGroups().getFirst();
    assertThat(mappedGroup.name()).isEqualTo("Bordas");
    assertThat(mappedGroup.minSelect()).isEqualTo(1);
    assertThat(mappedGroup.maxSelect()).isEqualTo(2);
    assertThat(mappedGroup.required()).isTrue();
    assertThat(mappedGroup.items()).hasSize(1);
    assertThat(mappedGroup.items().getFirst().name()).isEqualTo("Catupiry");
    assertThat(mappedGroup.items().getFirst().extraPrice()).isEqualByComparingTo("8.00");
    assertThat(mappedGroup.items().getFirst().sortOrder()).isEqualTo(10);
  }

  @Test
  void shouldMapSelectionRulesAndFilterInactiveItems() {
    var product =
        new MerchantViews.PublicStoreMenuProductView(
            UUID.randomUUID(),
            "Pizza Frango",
            "Descricao",
            new BigDecimal("39.90"),
            null,
            false,
            List.of(
                new MerchantViews.PublicStoreMenuOptionGroupView(
                    UUID.randomUUID(),
                    "Molhos",
                    0,
                    3,
                    false,
                    List.of(
                        new MerchantViews.PublicStoreMenuOptionItemView(
                            UUID.randomUUID(), "Barbecue", new BigDecimal("2.50"), 5)))));

    var response = PublicStoreMapper.toMenuProductOutput(product);

    assertThat(response.optionGroups()).hasSize(1);
    var mappedGroup = response.optionGroups().getFirst();
    assertThat(mappedGroup.minSelect()).isZero();
    assertThat(mappedGroup.maxSelect()).isEqualTo(3);
    assertThat(mappedGroup.required()).isFalse();
    assertThat(mappedGroup.items()).extracting(item -> item.name()).containsExactly("Barbecue");
  }

  private MerchantViews.StoreView store() {
    return new MerchantViews.StoreView(
        UUID.randomUUID(),
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo",
        StoreStatus.ACTIVE,
        Instant.parse("2026-03-20T10:00:00Z"));
  }
}
