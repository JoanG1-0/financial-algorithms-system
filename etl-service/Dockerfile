# =============================================================================
# Etapa 1: Compilación con Maven
# =============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /build

# Copia POMs de todos los módulos — Maven requiere que el reactor esté completo
# aunque solo compilemos etl-service
COPY pom.xml .
COPY etl-service/pom.xml etl-service/pom.xml
COPY algorithm-service/pom.xml algorithm-service/pom.xml
COPY report-service/pom.xml report-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
COPY config-server/pom.xml config-server/pom.xml

# Descarga dependencias del módulo (aprovecha caché de Docker)
RUN mvn -pl etl-service dependency:resolve -DskipTests -q

# Copia fuentes y genera el fat JAR ejecutable de Spring Boot
# spring-boot:repackage es explícito porque el proyecto no usa spring-boot-starter-parent
COPY etl-service/src etl-service/src
RUN mvn -pl etl-service package spring-boot:repackage -DskipTests -q

# =============================================================================
# Etapa 2: Runtime mínimo (JRE Alpine)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /build/etl-service/target/etl-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
