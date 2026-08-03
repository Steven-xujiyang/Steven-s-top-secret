package com.quant.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing kernel dependency rules.
 *
 * <p>These tests fail at BUILD TIME if any forbidden dependency
 * (Spring, JPA, HTTP client, JDBC driver) enters the kernel classpath.
 */
class KernelDependencyTest {

    private static JavaClasses kernelClasses;

    @BeforeAll
    static void setUp() {
        kernelClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.quant.kernel");
    }

    @Test
    void kernelHasNoSpringJpaOrHttpDependencies() {
        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.servlet..",
                        "org.hibernate..",
                        "java.sql..",
                        "javax.sql..",
                        "org.apache.http..",
                        "java.net.http..",
                        "com.fasterxml.jackson..",
                        "com.google.gson.."
                )
                .because("quant-kernel must be pure Java with no framework dependencies");

        rule.check(kernelClasses);
    }

    @Test
    void strategyDoesNotDependOnExecutionOrAccounting() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.quant.kernel.strategy")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.quant.kernel.execution..",
                        "com.quant.kernel.accounting.."
                )
                .because("strategy must not depend on execution or accounting details");

        rule.check(kernelClasses);
    }

    @Test
    void executionDoesNotDependOnConcreteStrategy() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.quant.kernel.execution")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.quant.kernel.strategy")
                .because("execution must not depend on concrete strategy types");

        rule.check(kernelClasses);
    }
}
