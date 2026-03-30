package com.kfood.architecture;

import org.junit.jupiter.api.Test;

class ArchUnitCompatibilityTest {

  @Test
  void shouldExecuteArchitectureRulesWithArchUnitFilter() {
    var architectureTest = new ArchitectureTest();

    architectureTest.domainShouldNotDependOnApiOrInfra();
    architectureTest.applicationLayerShouldNotDependOnHttpApiDtos();
    architectureTest.applicationContractsShouldNotDependOnInfraImplementations();
    architectureTest.paymentApplicationShouldNotDependOnInfraImplementations();
    architectureTest.orderUseCasesShouldNotDependOnInfraImplementations();
    architectureTest.merchantApplicationShouldNotDependOnInfraImplementations();
    architectureTest.merchantUserApplicationShouldNotDependOnInfraImplementations();
    architectureTest.paymentGatewayApplicationShouldNotDependOnApiOrInfra();
    architectureTest.catalogAvailabilityShouldNotDependOnInfra();
    architectureTest.catalogSelectionShouldNotDependOnInfra();
  }
}
