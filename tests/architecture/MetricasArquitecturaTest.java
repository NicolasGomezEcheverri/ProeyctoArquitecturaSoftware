package architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.metrics.ArchitectureMetrics;
import com.tngtech.archunit.library.metrics.ComponentDependencyMetrics;
import com.tngtech.archunit.library.metrics.MetricsComponent;
import com.tngtech.archunit.library.metrics.MetricsComponents;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Metricas de acoplamiento de Robert C. Martin (Richards &amp; Ford, cap. 3) calculadas con
 * ArchUnit: Ca, Ce, inestabilidad I, abstraccion A y distancia a la secuencia principal D.
 * Genera target/metricas/modularidad.md, que el pipeline publica como artefacto.
 */
class MetricasArquitecturaTest {

    private static final List<String> MODULOS =
            List.of("presentation", "session", "gameplay", "rules", "deck", "scoring", "history", "shared");
    private static final List<String> PAQUETES_REGLAS =
            List.of("rules.api", "rules.registry", "rules.evaluacion", "rules.clasica", "rules.apuntado");
    private static final double DISTANCIA_MAXIMA_PLUGIN = 0.35;

    private static JavaClasses clases;

    @BeforeAll
    static void importar() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.apuntado");
    }

    @Test
    @DisplayName("Reporte de modularidad por modulo y por paquete de reglas")
    void reporte() throws IOException {
        StringBuilder md = new StringBuilder("# Metricas de modularidad\n\n");
        md.append("Generado por MetricasArquitecturaTest (ArchUnit ").append(version()).append(").\n\n");
        md.append("## Modulos del monolito\n\n").append(tabla(componentes(MODULOS)));
        md.append("\n## Paquetes del modulo rules\n\n").append(tabla(componentes(PAQUETES_REGLAS)));
        Path salida = Path.of("target", "metricas", "modularidad.md");
        Files.createDirectories(salida.getParent());
        Files.writeString(salida, md.toString());
        System.getLogger("metricas").log(System.Logger.Level.INFO, "\n" + md);
        assertThat(salida).exists();
    }

    @Test
    @DisplayName("Las variantes de reglas quedan cerca de la secuencia principal")
    void pluginsCercaDeLaSecuenciaPrincipal() {
        ComponentDependencyMetrics m = ArchitectureMetrics.componentDependencyMetrics(componentes(PAQUETES_REGLAS));
        for (String plugin : List.of("rules.clasica", "rules.apuntado")) {
            assertThat(m.getNormalizedDistanceFromMainSequence(plugin))
                    .as("D de %s", plugin).isLessThanOrEqualTo(DISTANCIA_MAXIMA_PLUGIN);
        }
    }

    @Test
    @DisplayName("Ningun modulo depende de presentation (Ca de presentation = 0)")
    void presentationEsHoja() {
        ComponentDependencyMetrics m = ArchitectureMetrics.componentDependencyMetrics(componentes(MODULOS));
        assertThat(m.getAfferentCoupling("presentation")).isZero();
    }

    private static MetricsComponents<JavaClass> componentes(List<String> nombres) {
        List<MetricsComponent<JavaClass>> lista = new ArrayList<>();
        for (String nombre : nombres) {
            String paquete = "com.apuntado." + nombre;
            Set<JavaClass> delPaquete = clases.stream()
                    .filter(c -> c.getPackageName().equals(paquete) || c.getPackageName().startsWith(paquete + "."))
                    .collect(Collectors.toSet());
            lista.add(MetricsComponent.of(nombre, delPaquete));
        }
        return MetricsComponents.of(lista);
    }

    private static String tabla(MetricsComponents<JavaClass> componentes) {
        ComponentDependencyMetrics m = ArchitectureMetrics.componentDependencyMetrics(componentes);
        StringBuilder sb = new StringBuilder("| Componente | Clases | Ca | Ce | I | A | D | Zona |\n");
        sb.append("|---|---|---|---|---|---|---|---|\n");
        for (MetricsComponent<JavaClass> c : componentes) {
            String id = c.getIdentifier();
            double i = m.getInstability(id);
            double a = m.getAbstractness(id);
            double d = m.getNormalizedDistanceFromMainSequence(id);
            sb.append(String.format(Locale.ROOT, "| %s | %d | %d | %d | %.2f | %.2f | %.2f | %s |%n", id,
                    c.getElements().size(), m.getAfferentCoupling(id), m.getEfferentCoupling(id), i, a, d,
                    zona(i, a, d)));
        }
        return sb.toString();
    }

    private static String zona(double inestabilidad, double abstraccion, double distancia) {
        if (distancia <= 0.35) {
            return "cerca de la secuencia principal";
        }
        return inestabilidad + abstraccion < 1 ? "hacia la Zona del Dolor" : "hacia la Zona de Inutilidad";
    }

    private static String version() {
        Package p = ArchitectureMetrics.class.getPackage();
        return p.getImplementationVersion() == null ? "1.3" : p.getImplementationVersion();
    }
}
