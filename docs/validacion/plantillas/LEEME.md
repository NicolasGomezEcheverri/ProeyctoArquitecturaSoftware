# Cómo llenar las plantillas

Copia cada plantilla a `docs/validacion/datos/` antes de llenarla. Las plantillas se quedan vacías como referencia.

## observaciones.csv (una fila por participante y por tarea)

| Columna | Valores |
|---|---|
| id_usuario | U01, U02… |
| experiencia_previa | nunca / algunas / seguido |
| codigo_partida | el código de 5 letras de la mesa |
| modalidad | CLASICA o APUNTADO |
| tarea | T1 a T5 |
| resultado | si / no / con_ayuda |
| tiempo_s | segundos que tardó en completar la tarea |
| ayudas | cuántas veces el moderador tuvo que intervenir |
| errores_o_confusiones | lo que se observó, en una frase ("dudó entre mazo y pozo") |
| comentarios | lo que dijo en voz alta y vale la pena citar |

## cuestionario.csv (una fila por participante)

`sus1` a `sus10` y `e1` a `e4` van de 1 a 5. Las dos preguntas abiertas se copian textuales.

Usa punto y coma (`;`) como separador, igual que los CSV que exporta el juego.
