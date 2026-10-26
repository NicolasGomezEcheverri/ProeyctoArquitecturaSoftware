# Proyecto Apuntado - Arquitectura de Software

Este repositorio contiene la estructura base y las pruebas de gobernanza arquitectónica del proyecto **Apuntado**, implementado bajo el estilo de **Monolito Modular**.

## Requisitos Previos

Para descargar, compilar y probar este proyecto, necesitas tener instaladas las siguientes herramientas en tu entorno de desarrollo:

*   **Git** instalado en el sistema.
*   **Java Development Kit (JDK) 17** o superior (asegúrate de que la variable de entorno `JAVA_HOME` esté configurada).
*   **Apache Maven 3.8** o superior instalado y disponible en el `PATH` de tu sistema.

## Descarga del Repositorio

Abre una terminal, clona el proyecto y navega hacia su directorio ejecutando los siguientes comandos:

```bash
git clone https://github.com/NicolasGomezEcheverri/ProeyctoArquitecturaSoftware.git
cd ProeyctoArquitecturaSoftware
```

## Ejecución de Pruebas y Gobernanza

El proyecto integra **ArchUnit** para validar las fronteras de los módulos (Fitness Functions) y **Checkstyle** para garantizar que se respeten los límites de complejidad ciclomática del código.

### Pruebas unitarias y estructurales
Ejecuta el siguiente comando para compilar el proyecto y correr la suite de pruebas (JUnit):

```bash
mvn clean test
```
Este paso ejecutará el archivo `ArchitectureFitnessTest` para garantizar la ausencia de dependencias cíclicas y el respeto por los contratos de las reglas de juego.

### Validación completa (Tests + Checkstyle)
Para ejecutar tanto las pruebas como el análisis de calidad estático, utiliza:

```bash
mvn clean verify
```
*Nota: Si algún método en el paquete de reglas supera la complejidad ciclomática máxima permitida (10), la construcción fallará automáticamente, previniendo la degradación estructural del código.*

## Desarrollo en IDE

Para modificar el código o revisar la implementación, abre la carpeta raíz (donde se ubica el archivo `pom.xml`) utilizando cualquier IDE compatible con proyectos Java y Maven (como IntelliJ IDEA, Eclipse o VS Code). Esto permitirá que las dependencias se sincronicen y resuelvan de forma automática.