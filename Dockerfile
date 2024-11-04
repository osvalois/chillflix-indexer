# Build stage
FROM maven:3.8.4-openjdk-17-slim as build

WORKDIR /workspace/app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

COPY --from=build /workspace/app/target/*.jar app.jar
RUN chown -R spring:spring /app

USER spring

# Asegúrate de que estas variables de entorno estén configuradas
ENV PORT=8080
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

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]