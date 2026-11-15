# Diagramas del Entregable 3

| Archivo | Sección del informe |
|---|---|
| `1.1-despliegue.*` | 1.1 Diagrama UML de despliegue |
| `1.2-secuencia-cierre.*` | 1.2 Diagrama UML de secuencia del cierre de ronda y la liquidación |
| `1.3-clases-implementacion.*` | 1.3 Diagrama UML de clases de implementación |

Las fuentes son PlantUML (`.puml`). Para regenerar las imágenes:

```bash
java -jar plantuml.jar -tpng -tsvg docs/diagramas/*.puml
```

El diagrama de clases es ancho; el `.svg` se puede ampliar sin perder nitidez.
