# Diagramas del Entregable 3

| Archivo | Sección del informe |
|---|---|
| `1.1-despliegue.*` | 1.1 Diagrama UML de despliegue |
| `1.2-secuencia-cierre.*` | 1.2 Diagrama UML de secuencia del cierre de ronda y la liquidación |
| `1.3a-clases-nucleo.*` | 1.3 Clases de implementación, vista 1: núcleo de orquestación (gameplay) |
| `1.3b-clases-reglas.*` | 1.3 Clases de implementación, vista 2: motor de reglas (Strategy + ServiceLoader) |
| `1.3-clases-implementacion.*` | 1.3 Las dos vistas juntas en un solo diagrama (muy ancho; útil en SVG) |

Las fuentes son PlantUML (`.puml`). También están en draw.io (`.drawio`, con cada figura editable); `diagramas-entrega-3.drawio` trae los cuatro diagramas como páginas de un solo archivo. Para regenerar las imágenes:

```bash
java -jar plantuml.jar -tpng -tsvg docs/diagramas/*.puml
```

Para el informe se usan las vistas 1.3a y 1.3b; el diagrama completo queda como referencia.
