package com.kfood.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
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

  private static final DescribedPredicate<JavaClass> APPLICATION_CONTRACT_CLASSES =
      new DescribedPredicate<>("application contract classes") {
        @Override
        public boolean test(JavaClass input) {
          var name = input.getSimpleName();
          return input.getPackageName().startsWith("com.kfood.payment.app.port")
              || input.getPackageName().startsWith("com.kfood.payment.app.gateway")
              || input.getPackageName().startsWith("com.kfood.order.app.port")
              || input.getPackageName().startsWith("com.kfood.merchant.app.port")
              || input.getPackageName().startsWith("com.kfood.merchant.application")
              || ((input.getPackageName().startsWith("com.kfood.payment.app")
                      || input.getPackageName().startsWith("com.kfood.order.app")
                      || input.getPackageName().startsWith("com.kfood.merchant.app"))
                  && (name.endsWith("Command")
                      || name.endsWith("Output")
                      || name.endsWith("Result")
                      || name.endsWith("Query")));
        }
      };

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
  void applicationLayerShouldNotDependOnHttpApiDtos() {
    noClasses()
        .that()
        .resideInAnyPackage(
            "com.kfood.payment.app..",
            "com.kfood.order.app..",
            "com.kfood.merchant.app..",
            "com.kfood.merchant.application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..api..")
        .check(importedClasses);
  }

  @Test
  void applicationContractsShouldNotDependOnInfraImplementations() {
    noClasses()
        .that(APPLICATION_CONTRACT_CLASSES)
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..infra..")
        .check(importedClasses);
  }

  @Test
  void paymentApplicationShouldNotDependOnInfraImplementations() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.payment.app..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..infra..")
        .check(importedClasses);
  }

  @Test
  void orderUseCasesShouldNotDependOnInfraImplementations() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.order.app..")
        .and()
        .haveSimpleNameEndingWith("UseCase")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..infra..")
        .check(importedClasses);
  }

  @Test
  void merchantUseCasesShouldNotDependOnInfraImplementations() {
    noClasses()
        .that()
        .resideInAPackage("com.kfood.merchant.app..")
        .and()
        .haveSimpleNameEndingWith("UseCase")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..infra..")
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
