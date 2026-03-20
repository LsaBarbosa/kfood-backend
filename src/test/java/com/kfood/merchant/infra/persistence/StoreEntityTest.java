package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
            "America/Sao_Paulo");

    assertThat(store.getId()).isEqualTo(id);
    assertThat(store.getName()).isEqualTo("Loja do Bairro");
    assertThat(store.getSlug()).isEqualTo("loja-do-bairro");
    assertThat(store.getCnpj()).isEqualTo("45.723.174/0001-10");
    assertThat(store.getPhone()).isEqualTo("21999990000");
    assertThat(store.getTimezone()).isEqualTo("America/Sao_Paulo");
    assertThat(store.getStatus()).isEqualTo(StoreStatus.SETUP);
    assertThat(store.getHoursVersion()).isZero();

    store.changeName("Loja Centro");
    store.changeSlug("loja-centro");
    store.changeCnpj("54.550.752/0001-55");
    store.changePhone("21888887777");
    store.changeTimezone("UTC");

    assertThat(store.getName()).isEqualTo("Loja Centro");
    assertThat(store.getSlug()).isEqualTo("loja-centro");
    assertThat(store.getCnpj()).isEqualTo("54.550.752/0001-55");
    assertThat(store.getPhone()).isEqualTo("21888887777");
    assertThat(store.getTimezone()).isEqualTo("UTC");

    store.activate();
    assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);

    store.suspend();
    assertThat(store.getStatus()).isEqualTo(StoreStatus.SUSPENDED);

    store.moveToSetup();
    assertThat(store.getStatus()).isEqualTo(StoreStatus.SETUP);

    store.incrementHoursVersion();
    assertThat(store.getHoursVersion()).isEqualTo(1);
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
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<Store> constructor = Store.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }
}
