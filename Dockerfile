# ============================================================
#  Etapa 1: build del JAR con Gradle (imagen con JDK 21)
# ============================================================
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiamos primero los archivos de Gradle para aprovechar la caché de capas.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Ahora el código fuente y construimos el bootJar (sin correr tests aquí).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ============================================================
#  Etapa 2: runtime liviano (solo JRE) y usuario no-root
# ============================================================
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Usuario sin privilegios: buena práctica de seguridad en contenedores.
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /app/build/libs/*.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
