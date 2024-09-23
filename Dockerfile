# Use the official Maven image as a parent image
FROM maven:3.8.4-openjdk-17-slim as build

# Set the working directory in the container
WORKDIR /workspace/app

# Copy the pom.xml file
COPY pom.xml .

# Copy the project files
COPY src ./src

# Build the application and skip tests
RUN mvn clean package -DskipTests

# Use a smaller base image for the runtime
FROM eclipse-temurin:17-jre-alpine

# Add a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Set the working directory
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /workspace/app/target/*.jar app.jar

# Change ownership of the app directory
RUN chown -R spring:spring /app

# Use the non-root user
USER spring

# Expose the application port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Dspring.main.lazy-initialization=true -Dspring.jpa.open-in-view=false -Dspring.data.jpa.repositories.bootstrap-mode=lazy -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app/app.jar"]