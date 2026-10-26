package architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.apuntado",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureFitnessTest {

    // Regla 1A: presentation no accede al dominio ni a la persistencia
    @ArchTest
    public static final ArchRule presentation_no_accede_a_dominio_ni_persistencia =
            noClasses().that().resideInAPackage("com.apuntado.presentation..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.apuntado.deck..", "com.apuntado.rules..",
                            "com.apuntado.scoring..", "com.apuntado.history..",
                            "..internal..", "jakarta.persistence..");

    // Regla 1B: gameplay y el dominio no dependen de presentation (inversion de dependencias)
    @ArchTest
    public static final ArchRule dominio_no_depende_de_presentation =
            noClasses().that().resideInAnyPackage(
                            "com.apuntado.gameplay..", "com.apuntado.deck..",
                            "com.apuntado.rules..", "com.apuntado.scoring..",
                            "com.apuntado.history..")
                    .should().dependOnClassesThat().resideInAPackage("com.apuntado.presentation..");

    // Regla 2A: ReglaClasica no depende de ReglaApuntado
    @ArchTest
    public static final ArchRule clasica_no_depende_de_apuntado =
            noClasses().that().resideInAPackage("com.apuntado.rules.clasica..")
                    .should().dependOnClassesThat().resideInAPackage("com.apuntado.rules.apuntado..");

    // Regla 2B: ReglaApuntado no depende de ReglaClasica
    @ArchTest
    public static final ArchRule apuntado_no_depende_de_clasica =
            noClasses().that().resideInAPackage("com.apuntado.rules.apuntado..")
                    .should().dependOnClassesThat().resideInAPackage("com.apuntado.rules.clasica..");

    // Regla 2C: gameplay solo conoce el contrato rules.api
    @ArchTest
    public static final ArchRule gameplay_solo_conoce_rules_api =
            noClasses().that().resideInAPackage("com.apuntado.gameplay..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.apuntado.rules.registry..", "com.apuntado.rules.evaluacion..",
                            "com.apuntado.rules.clasica..", "com.apuntado.rules.apuntado..");

    // Regla 2D: sin dependencias ciclicas entre modulos
    @ArchTest
    public static final ArchRule modulos_sin_ciclos =
            slices().matching("com.apuntado.(*)..").should().beFreeOfCycles();
}
