package com.apuntado.scoring.internal;

/** EliminationEvaluator, RN-09: queda eliminado quien alcanza o supera el umbral. */
final class EvaluadorEliminacion {

    boolean quedaEliminado(int acumulado, int umbral) {
        return acumulado >= umbral;
    }
}
