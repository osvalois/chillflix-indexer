# Detect OS (MacOS/Linux/Windows)
OS := $(shell uname -s)

# Load environment variables from a .env file if present
ifneq (,$(wildcard .env))
    include .env
    export $(shell sed 's/=.*//' .env)
endif

# MacOS/Linux/Windows specific setup
ifeq ($(OS),Darwin) # MacOS
    R2DBC_URL=$(shell echo $$R2DBC_URL)
    R2DBC_USERNAME=$(shell echo $$R2DBC_USERNAME)
    R2DBC_PASSWORD=$(shell echo $$R2DBC_PASSWORD)
else ifeq ($(OS),Linux) # Linux
    R2DBC_URL=$(shell echo $$R2DBC_URL)
    R2DBC_USERNAME=$(shell echo $$R2DBC_USERNAME)
    R2DBC_PASSWORD=$(shell echo $$R2DBC_PASSWORD)
else # Windows (PowerShell)
    R2DBC_URL=$(shell echo $$env:R2DBC_URL)
    R2DBC_USERNAME=$(shell echo $$env:R2DBC_USERNAME)
    R2DBC_PASSWORD=$(shell echo $$env:R2DBC_PASSWORD)
endif

# Docker settings
IMAGE_NAME = chillflix-indexer
IMAGE_TAG = latest
DOCKER_PORT = 8090
DOCKER_HUB_USERNAME = osvalois
DOCKER_HUB_REPO = chillflix-indexer

# General settings
run:
	@echo "Iniciando aplicación en MacOS..."
	@echo "R2DBC URL: ${R2DBC_URL}"
	@echo "Server Port: ${SERVER_PORT}"
	./mvnw spring-boot:run \
		-Dspring.r2dbc.url=${R2DBC_URL} \
		-Dspring.r2dbc.username=${R2DBC_USERNAME} \
		-Dspring.r2dbc.password=${R2DBC_PASSWORD} \
		-Dserver.port=${SERVER_PORT}

# Maven package
package:
	@echo "Building package..."
	./mvnw clean package -DskipTests

# Clean and reset the environment
clean:
	@echo "Cleaning environment..."
	./mvnw clean

# Docker build
docker-build:
	@echo "Building Docker image: $(IMAGE_NAME):$(IMAGE_TAG)"
	docker build -t $(IMAGE_NAME):$(IMAGE_TAG) .

# Docker run
docker-run:
	@echo "Running Docker container with .env file..."
	docker run --rm -p $(DOCKER_PORT):8080 --env-file .env --name $(IMAGE_NAME) $(IMAGE_NAME):$(IMAGE_TAG)

# Docker stop
docker-stop:
	@echo "Stopping Docker container..."
	docker stop $(IMAGE_NAME) || true

# Docker Hub login
docker-login:
	@echo "Logging into Docker Hub..."
	docker login -u $(DOCKER_HUB_USERNAME)

# Docker tag for Docker Hub
docker-tag:
	@echo "Tagging Docker image for Docker Hub..."
	docker tag $(IMAGE_NAME):$(IMAGE_TAG) $(DOCKER_HUB_USERNAME)/$(DOCKER_HUB_REPO):$(IMAGE_TAG)

# Docker push to Docker Hub
docker-push: docker-tag
	@echo "Pushing Docker image to Docker Hub..."
	docker push $(DOCKER_HUB_USERNAME)/$(DOCKER_HUB_REPO):$(IMAGE_TAG)

# Docker full cycle (build and run)
docker: docker-build docker-run

# Docker full DevSecOps cycle (build, scan, tag, push)
docker-devsecops: docker-build docker-security docker-tag docker-push

# Docker security scan
docker-security:
	@echo "Running security scan on Docker image..."
	@echo "Note: This requires snyk CLI to be installed"
	-snyk container test $(IMAGE_NAME):$(IMAGE_TAG) --file=Dockerfile

# Show help
help:
	@echo "Available targets:"
	@echo "  run             - Run the application locally using Maven"
	@echo "  clean           - Clean the project"
	@echo "  package         - Build the project jar file"
	@echo "  docker-build    - Build Docker image"
	@echo "  docker-run      - Run Docker container with .env file"
	@echo "  docker-stop     - Stop Docker container"
	@echo "  docker          - Build and run Docker container"
	@echo "  docker-login    - Login to Docker Hub"
	@echo "  docker-tag      - Tag image for Docker Hub"
	@echo "  docker-push     - Push image to Docker Hub"
	@echo "  docker-security - Run security scan on Docker image"
	@echo "  docker-devsecops- Full cycle: build, scan, tag, push"
	@echo "  help            - Show this help"
