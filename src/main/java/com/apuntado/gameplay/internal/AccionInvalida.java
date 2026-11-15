package com.apuntado.gameplay.internal;

/** Accion que rompe una regla de turno; se le responde solo a quien la intento. */
class AccionInvalida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    AccionInvalida(String mensaje) {
        super(mensaje);
    }
}
