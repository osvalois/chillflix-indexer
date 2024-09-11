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

# General settings
run:
    @echo "Starting the application with the following settings:"
    @echo "R2DBC URL: $(R2DBC_URL)"
    @echo "R2DBC Username: $(R2DBC_USERNAME)"
    @echo "Server Port: $(SERVER_PORT)"
    ./gradlew bootRun -Dspring.r2dbc.url=$(R2DBC_URL) \
                      -Dspring.r2dbc.username=$(R2DBC_USERNAME) \
                      -Dspring.r2dbc.password=$(R2DBC_PASSWORD) \
                      -Dserver.port=$(SERVER_PORT)

# Clean and reset the environment
clean:
    @echo "Cleaning environment..."
    ./gradlew clean
