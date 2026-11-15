package com.apuntado.presentation;

import com.apuntado.shared.Carta;
import java.util.List;

/** Lo que manda el navegador por el WebSocket. */
record MensajeCliente(String tipo, String accionId, Carta carta, List<Carta> orden, Long ms) {
}
