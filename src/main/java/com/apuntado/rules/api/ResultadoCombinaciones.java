package com.apuntado.rules.api;

import com.apuntado.shared.Carta;
import java.util.List;

public record ResultadoCombinaciones(int ternas, int cuartas, List<Carta> sueltas) { }
