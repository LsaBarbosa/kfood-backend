package com.kfood.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.kfood";

    private final JavaClasses importedClasses =
        new ClassFileImporter().importPackages(BASE_PACKAGE);

    @Test
    void domainShouldNotDependOnApiApplicationOrInfra() {
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..api..", "..application..", "..infra..")
            .allowEmptyShould(true)
            .check(importedClasses);
    }

    @Test
    void applicationShouldNotDependOnApiOrInfra() {
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..api..", "..infra..")
            .allowEmptyShould(true)
            .check(importedClasses);
    }

    @Test
    void apiShouldNotDependDirectlyOnInfra() {
        noClasses()
            .that()
            .resideInAPackage("..api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infra..")
            .allowEmptyShould(true)
            .check(importedClasses);
    }
}
