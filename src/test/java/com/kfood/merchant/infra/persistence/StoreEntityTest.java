package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.domain.StoreStatus;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreEntityTest {

  @Test
  void shouldExposeStateAndApplyTransitions() {
    var id = UUID.randomUUID();
    var store =
        new Store(
            id,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo",
            StoreCategory.PIZZARIA,
            new StoreAddress("25000-000", "Rua Central", "100", "Centro", "Mage", "RJ"));

    assertThat(store.getId()).isEqualTo(id);
    assertThat(store.getName()).isEqualTo("Loja do Bairro");
    assertThat(store.getSlug()).isEqualTo("loja-do-bairro");
    assertThat(store.getCnpj()).isEqualTo("45.723.174/0001-10");
    assertThat(store.getPhone()).isEqualTo("21999990000");
    assertThat(store.getTimezone()).isEqualTo("America/Sao_Paulo");
    assertThat(store.getCategory()).isEqualTo(StoreCategory.PIZZARIA);
    assertThat(store.getAddress().getZipCode()).isEqualTo("25000000");
    assertThat(store.getStatus()).isEqualTo(StoreStatus.SETUP);
    assertThat(store.getHoursVersion()).isZero();
    assertThat(store.isCashPaymentEnabled()).isFalse();

    store.changeName("Loja Centro");
    store.changeSlug("loja-centro");
    store.changeCnpj("54.550.752/0001-55");
    store.changePhone("21888887777");
    store.changeTimezone("UTC");
    store.changeCategory(StoreCategory.PIZZARIA);
    store.changeAddress(new StoreAddress("26000-000", "Rua Norte", "200", "Bairro", "Mage", "rj"));

    assertThat(store.getName()).isEqualTo("Loja Centro");
    assertThat(store.getSlug()).isEqualTo("loja-centro");
    assertThat(store.getCnpj()).isEqualTo("54.550.752/0001-55");
    assertThat(store.getPhone()).isEqualTo("21888887777");
    assertThat(store.getTimezone()).isEqualTo("UTC");
    assertThat(store.getAddress().getZipCode()).isEqualTo("26000000");
    assertThat(store.getAddress().getState()).isEqualTo("RJ");

    store.activate();
    assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);

    store.suspend();
    assertThat(store.getStatus()).isEqualTo(StoreStatus.SUSPENDED);

    store.incrementHoursVersion();
    assertThat(store.getHoursVersion()).isEqualTo(1);

    store.enableCashPayment();
    assertThat(store.isCashPaymentEnabled()).isTrue();

    store.disableCashPayment();
    assertThat(store.isCashPaymentEnabled()).isFalse();
  }

  @Test
  void shouldNormalizeFieldsWhenChangingData() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    store.changeName(" Loja Centro ");
    store.changeSlug(" loja-centro ");
    store.changeCnpj(" 54.550.752/0001-55 ");
    store.changePhone(" 21888887777 ");
    store.changeTimezone(" UTC ");

    assertThat(store.getName()).isEqualTo("Loja Centro");
    assertThat(store.getSlug()).isEqualTo("loja-centro");
    assertThat(store.getCnpj()).isEqualTo("54.550.752/0001-55");
    assertThat(store.getPhone()).isEqualTo("21888887777");
    assertThat(store.getTimezone()).isEqualTo("UTC");
  }

  @Test
  void shouldApplySetupWhenStatusIsNullAtPrePersist() throws Exception {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    Field status = Store.class.getDeclaredField("status");
    status.setAccessible(true);
    status.set(store, null);

    Method prePersist = Store.class.getDeclaredMethod("prePersist");
    prePersist.setAccessible(true);
    prePersist.invoke(store);

    assertThat(store.getStatus()).isEqualTo(StoreStatus.SETUP);
  }

  @Test
  void shouldResetNegativeHoursVersionAtPrePersist() throws Exception {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    Field hoursVersion = Store.class.getDeclaredField("hoursVersion");
    hoursVersion.setAccessible(true);
    hoursVersion.set(store, -1);

    Method prePersist = Store.class.getDeclaredMethod("prePersist");
    prePersist.setAccessible(true);
    prePersist.invoke(store);

    assertThat(store.getHoursVersion()).isZero();
  }

  @Test
  void shouldExposeStatusFlags() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    assertThat(store.isSetup()).isTrue();
    assertThat(store.isActive()).isFalse();
    assertThat(store.isSuspended()).isFalse();

    store.activate();
    assertThat(store.isSetup()).isFalse();
    assertThat(store.isActive()).isTrue();

    store.suspend();
    assertThat(store.isSuspended()).isTrue();
  }

  @Test
  void shouldRejectInvalidActiveToActiveTransition() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    store.activate();

    assertThatThrownBy(store::activate)
        .isInstanceOf(StoreStatusTransitionException.class)
        .hasMessageContaining("Invalid store status transition");
  }

  @Test
  void shouldRejectInvalidSetupToSuspendedTransition() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    assertThatThrownBy(store::suspend)
        .isInstanceOf(StoreStatusTransitionException.class)
        .hasMessageContaining("Invalid store status transition");
  }

  @Test
  void shouldReactivateStoreFromSuspendedStatus() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    store.activate();
    store.suspend();
    store.activate();

    assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<Store> constructor = Store.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  @Test
  void shouldReportCategoryAndAddressCompleteness() {
    var completeStore =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo",
            StoreCategory.PIZZARIA,
            new StoreAddress("25000-000", "Rua Central", "100", "Centro", "Mage", "RJ"));
    var legacyStore =
        new Store(
            UUID.randomUUID(),
            "Loja Legada",
            "loja-legada",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    assertThat(completeStore.hasCategoryConfigured()).isTrue();
    assertThat(completeStore.hasAddressConfigured()).isTrue();
    assertThat(legacyStore.hasCategoryConfigured()).isFalse();
    assertThat(legacyStore.hasAddressConfigured()).isFalse();
  }
}
