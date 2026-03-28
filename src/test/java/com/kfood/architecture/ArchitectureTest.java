package com.kfood.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  private static final String BASE_PACKAGE = "com.kfood";

  private final JavaClasses importedClasses =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(BASE_PACKAGE);

  @Test
  void domainShouldNotDependOnApiOrInfra() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..api..", "..infra..")
        .check(importedClasses);
  }

  @Test
  void merchantDomainShouldNotDependOnApiOrInfra() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.merchant.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..api..", "..infra..")
        .check(importedClasses);
  }

  @Test
  void orderDomainShouldNotDependOnApiOrInfra() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.order.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..api..", "..infra..")
        .check(importedClasses);
  }

  @Test
  void paymentDomainShouldNotDependOnApiOrInfra() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.payment.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..api..", "..infra..")
        .check(importedClasses);
  }

  @Test
  void merchantUserApplicationShouldNotDependOnApi() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.merchant.application.user..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..api..")
        .check(importedClasses);
  }

  @Test
  void merchantUserApplicationShouldNotDependOnInfraImplementations() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.merchant.application.user..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..infra..")
        .check(importedClasses);
  }

  @Test
  void paymentApplicationShouldNotDependOnApi() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.payment.app..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..api..")
        .check(importedClasses);
  }

  @Test
  void paymentApplicationShouldNotDependOnConcreteInfraRepositories() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.payment.app..")
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("com.kfood.payment.infra.persistence.PaymentRepository")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName("com.kfood.order.infra.persistence.SalesOrderRepository")
        .check(importedClasses);
  }

  @Test
  void paymentGatewayApplicationShouldNotDependOnApiOrInfra() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.payment.app.gateway..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..api..", "..infra..")
        .check(importedClasses);
  }

  @Test
  void catalogAvailabilityShouldNotDependOnInfra() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.catalog.app.availability..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..infra..")
        .check(importedClasses);
  }

  @Test
  void catalogSelectionShouldNotDependOnInfra() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.catalog.app.selection..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..infra..")
        .check(importedClasses);
  }
}
