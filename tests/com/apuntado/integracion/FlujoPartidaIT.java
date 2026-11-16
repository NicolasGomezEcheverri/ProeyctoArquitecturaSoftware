package com.apuntado.integracion;

import static com.apuntado.soporte.Cartas.c;
import static org.assertj.core.api.Assertions.assertThat;

import com.apuntado.app.ApuntadoApplication;
import com.apuntado.gameplay.api.Credencial;
import com.apuntado.gameplay.api.EstadoPublico;
import com.apuntado.gameplay.api.IAccionesJugador;
import com.apuntado.gameplay.api.IConsultaPartida;
import com.apuntado.gameplay.api.MetricasParticipante;
import com.apuntado.gameplay.api.PenalizacionRonda;
import com.apuntado.gameplay.api.ResultadoAccion;
import com.apuntado.session.api.IGestionPartida;
import com.apuntado.session.api.IngresoSala;
import com.apuntado.session.api.SolicitudSala;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Pruebas de punta a punta del cierre de ronda con la base real (H2 en modo
 * PostgreSQL, mismas migraciones Flyway). Recorren el flujo del diagrama de
 * secuencia 1.2: orquestador -> regla -> puntaje -> base de datos.
 */
@SpringBootTest(classes = ApuntadoApplication.class)
@ActiveProfiles("local")
@Import(SoportePartidas.class)
class FlujoPartidaIT {

    // Quien abre: cuarta de sietes, escalera 2-3-4 de corazones y terna de K, mas el 9T para descartar.
    private static final String MANO_CLASICA = "7P 7C 7D 7T 2C 3C 4C KP KC KD 9T";
    // Quien abre en Apuntado: tres ternas, un 4T que le sobra y la QD para descartar.
    private static final String MANO_APUNTADO = "7P 7C 7D 2C 3C 4C KP KC KD 4T QD";
    // El otro jugador no logra ninguna combinacion: 10+3+5+9+10+2+4+6+8+10 = 67 puntos.
    private static final String MANO_SIN_JUEGO = "AP 3P 5P 9P JP 2D 4D 6D 8D 10D";

    @Autowired private IGestionPartida gestion;
    @Autowired private IAccionesJugador acciones;
    @Autowired private IConsultaPartida consultas;
    @Autowired private SoportePartidas.MazoArreglado mazo;
    @Autowired private SoportePartidas.NotificadorEnMemoria notificador;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("Clásica: rechaza jugadas fuera de regla, acepta 1 cuarta + 2 ternas + descarte y liquida")
    void cierreClasico() {
        Mesa mesa = crearMesa("CLASICA", 101, MANO_CLASICA);

        assertThat(acciones.robarDelMazo(mesa.abridor(), "a1").mensaje()).contains("Ya robaste");
        assertThat(acciones.descartar(mesa.otro(), c("AP"), "a2").mensaje()).contains("Espera tu turno");
        ResultadoAccion invalido = acciones.solicitarCierre(mesa.abridor(), c("7P"), "a3");
        assertThat(invalido.aceptada()).isFalse();
        assertThat(invalido.mensaje()).contains("1 cuarta y 2 ternas");

        ResultadoAccion valido = acciones.solicitarCierre(mesa.abridor(), c("9T"), "a4");
        assertThat(valido.aceptada()).isTrue();

        EstadoPublico estado = notificador.estado(mesa.codigo());
        assertThat(estado.estado()).isEqualTo("RONDA_TERMINADA");
        assertThat(estado.ultimaAccionId()).isEqualTo("a4");
        assertThat(penalizacion(estado, mesa.otro().jugadorId())).isEqualTo(67);
        assertThat(penalizacion(estado, mesa.abridor().jugadorId())).isZero();
        assertThat(estado.topePozo()).isEqualTo(c("9T"));
        assertThat(estado.movimiento().tipo()).isEqualTo("CIERRE");
        assertThat(estado.movimiento().carta()).isEqualTo(c("9T"));
        assertThat(estado.movimiento().jugadorId()).isEqualTo(mesa.abridor().jugadorId());

        int partidaId = partidaId(mesa.codigo());
        assertThat(jdbc.queryForObject("select count(*) from puntaje_acumulado p join ronda r on r.id = p.ronda_id "
                + "where r.partida_id = ?", Integer.class, partidaId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select cerrador_id from ronda where partida_id = ? and numero = 1",
                Integer.class, partidaId)).isEqualTo(mesa.abridor().jugadorId());
        assertThat(tiposDeEvento(partidaId)).contains("ACCION_RECHAZADA", "CIERRE_RECHAZADO", "CIERRE_ACEPTADO");

        ResultadoAccion siguiente = acciones.siguienteRonda(mesa.otro());
        assertThat(siguiente.aceptada()).isTrue();
        EstadoPublico ronda2 = notificador.estado(mesa.codigo());
        assertThat(ronda2.ronda()).isEqualTo(2);
        assertThat(ronda2.turnoDe()).as("la siguiente ronda la abre el otro jugador").isEqualTo(mesa.otro().jugadorId());
        assertThat(ronda2.abridorId()).isEqualTo(mesa.otro().jugadorId());
        assertThat(ronda2.movimiento().tipo()).isEqualTo("RONDA_NUEVA");
        assertThat(notificador.mano(mesa.codigo(), mesa.otro().jugadorId())).hasSize(11);
    }

    @Test
    @DisplayName("Apuntado: quien cierra apunta su sobrante y la partida termina al llegar al umbral")
    void apuntadoHastaElUmbral() {
        Mesa mesa = crearMesa("APUNTADO", 50, MANO_APUNTADO);

        ResultadoAccion cierre = acciones.solicitarCierre(mesa.abridor(), c("QD"), "b1");
        assertThat(cierre.aceptada()).isTrue();

        EstadoPublico estado = notificador.estado(mesa.codigo());
        assertThat(penalizacion(estado, mesa.abridor().jugadorId())).isEqualTo(4);
        assertThat(penalizacion(estado, mesa.otro().jugadorId())).isEqualTo(67);
        assertThat(estado.estado()).isEqualTo("FINALIZADA");
        assertThat(estado.ganadorId()).isEqualTo(mesa.abridor().jugadorId());

        Map<String, Object> partida = jdbc.queryForMap("select estado, ganador_id from partida where codigo = ?",
                mesa.codigo());
        assertThat(partida.get("estado")).isEqualTo("FINALIZADA");
        assertThat(((Number) partida.get("ganador_id")).intValue()).isEqualTo(mesa.abridor().jugadorId());
        assertThat(tiposDeEvento(partidaId(mesa.codigo()))).contains("JUGADOR_ELIMINADO", "PARTIDA_FINALIZADA");
        assertThat(acciones.siguienteRonda(mesa.otro()).aceptada()).isFalse();
    }

    @Test
    @DisplayName("Dos cierres simultáneos del mismo jugador: solo uno se liquida")
    void cierresSimultaneos() throws Exception {
        Mesa mesa = crearMesa("CLASICA", 101, MANO_CLASICA);
        Callable<ResultadoAccion> cerrar = () -> acciones.solicitarCierre(mesa.abridor(), c("9T"), null);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Future<ResultadoAccion>> resultados = hilos.invokeAll(List.of(cerrar, cerrar));
        hilos.shutdown();

        long aceptados = 0;
        for (Future<ResultadoAccion> r : resultados) {
            aceptados += r.get().aceptada() ? 1 : 0;
        }
        assertThat(aceptados).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from puntaje_acumulado p join ronda r on r.id = p.ronda_id "
                + "where r.partida_id = ?", Integer.class, partidaId(mesa.codigo()))).isEqualTo(2);
    }

    @Test
    @DisplayName("Las métricas de validación salen de la bitácora de la partida")
    void metricasDesdeLaBitacora() {
        Mesa mesa = crearMesa("CLASICA", 101, MANO_CLASICA);
        acciones.solicitarCierre(mesa.abridor(), c("KP"), "m1");
        acciones.solicitarCierre(mesa.abridor(), c("9T"), "m2");
        acciones.reportarLatencia(mesa.abridor(), 40);
        acciones.reportarLatencia(mesa.abridor(), 60);

        MetricasParticipante abridor = consultas.metricas(mesa.codigo()).stream()
                .filter(m -> m.jugadorId() == mesa.abridor().jugadorId()).findFirst().orElseThrow();
        assertThat(abridor.cierresIntentados()).isEqualTo(2);
        assertThat(abridor.cierresAceptados()).isEqualTo(1);
        assertThat(abridor.cierresRechazados()).isEqualTo(1);
        assertThat(abridor.turnos()).isEqualTo(1);
        assertThat(abridor.latenciaPromedioMs()).isEqualTo(50);
        assertThat(abridor.latenciaP95Ms()).isEqualTo(60);
        assertThat(consultas.bitacora(mesa.codigo())).isNotEmpty();
    }

    // ----------------------------------------------------------------------

    private Mesa crearMesa(String modalidad, int umbral, String manoAbridor) {
        mazo.prepararDosJugadores(manoAbridor, MANO_SIN_JUEGO);
        IngresoSala ana = gestion.crearSala(new SolicitudSala("Ana", modalidad, 2, umbral));
        IngresoSala beto = gestion.unirse(ana.codigo(), "Beto");
        EstadoPublico estado = notificador.estado(ana.codigo());
        Credencial credAna = new Credencial(ana.codigo(), ana.jugadorId(), ana.token());
        Credencial credBeto = new Credencial(beto.codigo(), beto.jugadorId(), beto.token());
        boolean abreAna = estado.turnoDe() == ana.jugadorId();
        return new Mesa(ana.codigo(), abreAna ? credAna : credBeto, abreAna ? credBeto : credAna);
    }

    private int penalizacion(EstadoPublico estado, int jugadorId) {
        return estado.ultimoCierre().penalizaciones().stream()
                .filter(p -> p.jugadorId() == jugadorId).mapToInt(PenalizacionRonda::puntos).findFirst().orElseThrow();
    }

    private int partidaId(String codigo) {
        return jdbc.queryForObject("select id from partida where codigo = ?", Integer.class, codigo);
    }

    private List<String> tiposDeEvento(int partidaId) {
        return jdbc.queryForList("select tipo from evento_auditoria where partida_id = ?", String.class, partidaId);
    }

    private record Mesa(String codigo, Credencial abridor, Credencial otro) {
    }
}
