package architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;

/**
 * Fitness functions del proyecto (Entregable 2, seccion 5; ampliadas en el Entregable 3).
 * Corren en cada build de CI: si alguien rompe una frontera, el pipeline queda en rojo.
 */
@AnalyzeClasses(packages = "com.apuntado",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureFitnessTest {

    private static final String[] MODULOS_DOMINIO = {"deck", "rules", "scoring", "history"};

    // ---------------------------------------------------------------------
    // Regla 1: aislamiento de capas (Prueba 2 de la guia del Entregable 3)
    // ---------------------------------------------------------------------

    // Regla 1A: presentation no accede al dominio ni a la persistencia
    @ArchTest
    public static final ArchRule presentation_no_accede_a_dominio_ni_persistencia =
            noClasses().that().resideInAPackage("com.apuntado.presentation..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.apuntado.deck..", "com.apuntado.rules..",
                            "com.apuntado.scoring..", "com.apuntado.history..",
                            "..internal..", "jakarta.persistence..",
                            "org.springframework.data..", "org.springframework.jdbc..", "java.sql..")
                    .because("la interfaz de usuario solo habla con session y gameplay; nunca con el almacenamiento");

    // Regla 1B: gameplay y el dominio no dependen de presentation (inversion de dependencias)
    @ArchTest
    public static final ArchRule dominio_no_depende_de_presentation =
            noClasses().that().resideInAnyPackage(
                            "com.apuntado.gameplay..", "com.apuntado.deck..",
                            "com.apuntado.rules..", "com.apuntado.scoring..",
                            "com.apuntado.history..")
                    .should().dependOnClassesThat().resideInAPackage("com.apuntado.presentation..");

    // Regla 1C: solo scoring accede a su implementacion interna (retroalimentacion E1, RN-11)
    @ArchTest
    public static final ArchRule solo_scoring_actualiza_el_puntaje =
            noClasses().that().resideOutsideOfPackages("com.apuntado.scoring..")
                    .should().dependOnClassesThat().resideInAPackage("com.apuntado.scoring.internal..")
                    .because("el marcador solo se modifica a traves de IPuntuacion");

    // Regla 1D: ningun modulo entra al paquete internal de otro (salvo la raiz de composicion)
    @ArchTest
    public static final ArchRule nadie_usa_el_internal_de_otro_modulo = CompositeArchRule.of(
            internalPrivado("session")).and(internalPrivado("gameplay")).and(internalPrivado("deck"))
            .and(internalPrivado("history"));

    // ---------------------------------------------------------------------
    // Regla 2: reglas de juego como plug-ins (Prueba 1) y ausencia de ciclos (Prueba 3)
    // ---------------------------------------------------------------------

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

    // Regla 2E: tampoco hay ciclos entre subpaquetes (api, internal, registry, clasica...)
    @ArchTest
    public static final ArchRule subpaquetes_sin_ciclos =
            slices().matching("com.apuntado.(**)").should().beFreeOfCycles();

    // Regla 2F: una variante solo conoce el contrato, el evaluador comun y los tipos compartidos
    @ArchTest
    public static final ArchRule variantes_solo_dependen_del_contrato = CompositeArchRule.of(
            varianteAislada("clasica")).and(varianteAislada("apuntado"));

    // Regla 2G: nadie fuera de rules (y la raiz de composicion) nombra una variante concreta
    @ArchTest
    public static final ArchRule variantes_solo_por_service_loader =
            noClasses().that().resideOutsideOfPackages("com.apuntado.rules..", "com.apuntado.app..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.apuntado.rules.clasica..", "com.apuntado.rules.apuntado..",
                            "com.apuntado.rules.registry..", "com.apuntado.rules.evaluacion..");

    // Regla 2H: los modulos de dominio no se llaman entre si; solo comparten shared (mediador, E2 1.2)
    @ArchTest
    public static final ArchRule dominio_sin_llamadas_laterales = CompositeArchRule.of(
            sinLlamadasLaterales("deck")).and(sinLlamadasLaterales("rules"))
            .and(sinLlamadasLaterales("scoring")).and(sinLlamadasLaterales("history"));

    // Regla 2I: el dominio no depende de quien lo orquesta (ADR-03, direccion de dependencias)
    @ArchTest
    public static final ArchRule dominio_no_conoce_al_orquestador =
            noClasses().that().resideInAnyPackage("..deck..", "..rules..", "..scoring..", "..history..")
                    .should().dependOnClassesThat().resideInAnyPackage("..gameplay..", "..session..",
                            "..presentation..");

    // Regla 2J: rules y deck son Java puro; no dependen de Spring ni de JPA
    @ArchTest
    public static final ArchRule reglas_y_mazo_sin_framework =
            noClasses().that().resideInAnyPackage("com.apuntado.rules..", "com.apuntado.deck..",
                            "com.apuntado.shared..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..");

    private static ArchRule internalPrivado(String modulo) {
        return noClasses().that().resideOutsideOfPackages("com.apuntado." + modulo + "..", "com.apuntado.app..")
                .should().dependOnClassesThat().resideInAPackage("com.apuntado." + modulo + ".internal..")
                .as("solo " + modulo + " y la raiz de composicion usan " + modulo + ".internal");
    }

    private static ArchRule varianteAislada(String variante) {
        return classes().that().resideInAPackage("com.apuntado.rules." + variante + "..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "com.apuntado.rules." + variante + "..", "com.apuntado.rules.api..",
                        "com.apuntado.rules.evaluacion..", "com.apuntado.shared..", "java..")
                .as("la variante " + variante + " solo depende del contrato y de tipos compartidos");
    }

    private static ArchRule sinLlamadasLaterales(String modulo) {
        String[] otros = java.util.Arrays.stream(MODULOS_DOMINIO)
                .filter(m -> !m.equals(modulo))
                .map(m -> "com.apuntado." + m + "..")
                .toArray(String[]::new);
        return noClasses().that().resideInAPackage("com.apuntado." + modulo + "..")
                .should().dependOnClassesThat().resideInAnyPackage(otros)
                .as(modulo + " no llama a otros modulos de dominio");
    }
}
