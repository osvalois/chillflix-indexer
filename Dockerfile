# ------------------------------------------------------------
# Etapa de build: compila la aplicación con Maven
# ------------------------------------------------------------
FROM maven:3.8.4-openjdk-17-slim AS build

# Definimos el directorio de trabajo en la imagen de build
WORKDIR /workspace/app

# Copiamos sólo el POM para aprovechar el cache de dependencias
COPY pom.xml .

# Bajamos todas las dependencias sin compilar
RUN mvn dependency:go-offline -B

# Copiamos el código fuente y compilamos sin ejecutar tests
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------
# Etapa de runtime: ejecuta el JAR resultante sobre Alpine
# ------------------------------------------------------------
# Usamos una imagen Alpine que realmente existe en Docker Hub
FROM openjdk:17-alpine AS runtime

# Creamos un usuario no-root para mayor seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Directorio de trabajo donde se ubicará la app
WORKDIR /app

# Copiamos el JAR construido desde la etapa de build
COPY --from=build /workspace/app/target/*.jar app.jar

# Ajustamos permisos
RUN chown -R spring:spring /app

# Cambiamos al usuario no-root
USER spring

# Exponemos el puerto por defecto de la app
ENV PORT=8080

# Variables de configuración de JVM y Spring
ENV JAVA_OPTS="\
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:MaxGCPauseMillis=100 \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=${PORT} \
    -Dspring.profiles.active=prod \
    -Dserver.address=0.0.0.0"

EXPOSE ${PORT}

# Entrypoint en formato exec para recibir señales correctamente
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
