package kg.bakai.stopfactor;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * The layers of the stop factor module: controller → service → repository → entity. A class in the wrong place, or a
 * call against the layers, fails the build.
 */
class ArchitectureTest {

    private static final String BASE = "kg.bakai.stopfactor";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    void nothingDependsOnControllers() {
        noClasses().that().resideOutsideOfPackage(BASE + ".controller..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".controller..")
                .check(CLASSES);
    }

    @Test
    void controllersDoNotUseRepositories() {
        noClasses().that().resideInAPackage(BASE + ".controller..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".repository..")
                .check(CLASSES);
    }

    @Test
    void serviceImplementationsAreUsedThroughTheirInterfaces() {
        noClasses().that().resideOutsideOfPackages(BASE + ".service.impl..", BASE + ".config..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".service.impl..")
                .check(CLASSES);
    }

    @Test
    void entitiesEnumsAndDtosDependOnNoLayer() {
        noClasses().that().resideInAnyPackage(BASE + ".entity..", BASE + ".enums..", BASE + ".dto..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".controller..", BASE + ".service..", BASE + ".repository..", BASE + ".validator..")
                .check(CLASSES);
    }

    @Test
    void calculatorsOnlyMeasureTheCheckRequest() {
        noClasses().that().resideInAPackage(BASE + ".calculator..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".controller..", BASE + ".service..", BASE + ".repository..", BASE + ".validator..")
                .check(CLASSES);
    }

    @Test
    void reportsAreReadWithoutTheDatabase() {
        noClasses().that().resideInAPackage(BASE + ".report..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".controller..", BASE + ".service..", BASE + ".repository..", BASE + ".entity..",
                        BASE + ".calculator..")
                .check(CLASSES);
    }

    @Test
    void repositoriesAreInterfaces() {
        classes().that().resideInAPackage(BASE + ".repository").should().beInterfaces().check(CLASSES);
    }

    @Test
    void packagesHaveNoCycles() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(CLASSES);
    }
}
