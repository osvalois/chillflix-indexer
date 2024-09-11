# Use the official Maven image as a parent image
FROM maven:3.8.4-openjdk-17-slim as build

# Set the working directory in the container
WORKDIR /workspace/app

# Copy the pom.xml file
COPY pom.xml .

# Download all required dependencies into one layer
RUN mvn dependency:go-offline -B

# Copy the project files
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Start with a base image containing Java runtime (usando una imagen más ligera)
FROM eclipse-temurin:17-jre-alpine

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Copy the jar file from the build stage
COPY --from=build /workspace/app/target/*.jar app.jar

# Run the jar file with optimizations
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","/app.jar"]

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Dspring.main.lazy-initialization=true -Dspring.jpa.open-in-view=false -Dspring.data.jpa.repositories.bootstrap-mode=lazy"