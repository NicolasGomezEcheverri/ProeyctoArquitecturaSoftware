# Etapa 1: compilar el jar con Maven (las pruebas corren en CI, no aqui)
FROM maven:3.9-eclipse-temurin-17 AS compilacion
WORKDIR /fuente
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY config config
COPY src src
RUN mvn -B -q -DskipTests -Dcheckstyle.skip -Dpmd.skip -Djacoco.skip package

# Etapa 2: imagen liviana solo con el JRE y el jar
FROM eclipse-temurin:17-jre
ENV LANG=C.UTF-8 TZ=America/Bogota
WORKDIR /app
COPY --from=compilacion /fuente/target/apuntado.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
