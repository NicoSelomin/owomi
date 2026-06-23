# ============================================================
# OWOMI Backend — Dockerfile multi-stage (build + runtime)
# ============================================================

# --- Étape 1 : build du jar avec Maven ---
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# Copie des sources et du wrapper Maven, puis build (cache des dépendances)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# --- Étape 2 : image runtime légère ---
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Utilisateur non-root pour la sécurité
RUN addgroup -g 1001 owomi && adduser -u 1001 -G owomi -s /bin/sh -D owomi

COPY --from=build /build/target/owomi-backend-*.jar app.jar
RUN chown owomi:owomi app.jar

USER owomi

EXPOSE 8080

# Options JVM adaptées aux conteneurs
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
