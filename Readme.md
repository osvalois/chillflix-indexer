# ChillFlix Indexer

![ChillFlix Logo](https://via.placeholder.com/150x150.png?text=ChillFlix+Logo)

[![Build Status](https://github.com/osvalois/chillflix-indexer/actions/workflows/docker-build-push.yml/badge.svg)](https://github.com/osvalois/chillflix-indexer/actions/workflows/docker-build-push.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=osvalois_chillflix-indexer&metric=alert_status)](https://sonarcloud.io/dashboard?id=osvalois_chillflix-indexer)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=osvalois_chillflix-indexer&metric=coverage)](https://sonarcloud.io/dashboard?id=osvalois_chillflix-indexer)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=osvalois_chillflix-indexer&metric=security_rating)](https://sonarcloud.io/dashboard?id=osvalois_chillflix-indexer)
[![Docker Pulls](https://img.shields.io/docker/pulls/osvalois/chillflix-indexer.svg?style=flat-square)](https://hub.docker.com/r/osvalois/chillflix-indexer)
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://opensource.org/licenses/MIT)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)

> A high-performance, reactive movie indexing and search service built with Spring WebFlux and R2DBC.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Architecture](#architecture)
- [Performance](#performance)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [Code of Conduct](#code-of-conduct)
- [License](#license)
- [Acknowledgements](#acknowledgements)

## Overview

ChillFlix Indexer is a cutting-edge, reactive microservice designed to efficiently index and search a vast collection of movies. Built on Spring WebFlux and utilizing R2DBC for non-blocking database operations, this service offers lightning-fast performance and excellent scalability.

## Features

- 🚀 High-performance, reactive architecture
- 🔍 Advanced search capabilities with full-text search
- 📊 Comprehensive movie metadata management
- 🔒 Secure API with rate limiting and circuit breaking
- 📈 Real-time statistics and analytics
- 🔄 Bulk operations for efficient data management
- 📡 Websocket support for real-time updates
- 🧪 Extensive test coverage ensuring reliability

## Getting Started

### Prerequisites

Ensure you have the following installed:

- Java JDK 17 or later
- Maven 3.6 or later
- PostgreSQL 13 or later
- Docker (optional, for containerization)

### Installation

1. Clone the repository:
   ```sh
   git clone https://github.com/chillflix/indexer.git
   cd indexer
   ```

2. Set up the database:
   ```sh
   psql -U postgres
   CREATE DATABASE chillflix;
   \q
   ```

3. Configure the application:
   Copy `.env.example` to `.env` and update the environment variables with your database credentials and other settings.

4. Run with Maven:
   ```sh
   # Build the project
   make package
   
   # Run locally using Maven
   make run
   ```

5. Run with Docker:
   ```sh
   # Build and run with Docker (uses .env file)
   make docker
   
   # Or build and run separately
   make docker-build
   make docker-run
   ```

6. See available commands:
   ```sh
   make help
   ```

## Usage

Once the application is running, you can interact with it via its RESTful API. Here's a quick example using curl:

```sh
# Search for movies
curl -X GET "http://localhost:8080/api/v1/movies/search?term=inception&page=0&size=10"

# Get movie by ID
curl -X GET "http://localhost:8080/api/v1/movies/{id}"

# Create a new movie
curl -X POST "http://localhost:8080/api/v1/movies" -H "Content-Type: application/json" -d '{
  "title": "Inception",
  "year": 2010,
  "director": "Christopher Nolan",
  "genre": ["Sci-Fi", "Action"],
  "rating": 8.8
}'
```

For more detailed usage instructions, please refer to our [API Documentation](#api-documentation).

## API Documentation

Our API is thoroughly documented using OpenAPI (Swagger). You can access the interactive API documentation by running the application and navigating to:

```
http://localhost:8080/swagger-ui.html
```

This documentation provides detailed information about each endpoint, including request/response formats, authentication requirements, and example usage.

## Architecture

ChillFlix Indexer follows a reactive, microservices-based architecture:

```mermaid
graph TD
    A[Client] -->|HTTP/WebSocket| B[API Gateway]
    B --> C[ChillFlix Indexer]
    C -->|R2DBC| D[PostgreSQL]
    C -->|Pub/Sub| E[Redis]
    C --> F[Elasticsearch]
```

Key components:
- **Spring WebFlux**: Provides the reactive web framework
- **R2DBC**: Enables non-blocking database operations
- **Elasticsearch**: Powers our advanced search capabilities
- **Redis**: Used for caching and pub/sub messaging

For a more detailed architectural overview, please see our [Architecture Document](docs/ARCHITECTURE.md).

## Performance

ChillFlix Indexer is designed for high performance:

- Handles 10,000+ concurrent requests
- Average response time < 50ms
- 99th percentile response time < 200ms

We regularly conduct performance testing and publish the results in our [Performance Reports](docs/PERFORMANCE.md).

## Testing

We maintain a comprehensive test suite including unit tests, integration tests, and end-to-end tests. To run the tests:

```sh
mvn test  # Run unit tests
mvn verify  # Run all tests including integration tests
```

For more information on our testing strategy, see [TESTING.md](docs/TESTING.md).

## Deployment

ChillFlix Indexer can be deployed in various environments:

### Docker Deployment

The application comes with Docker support:

1. Create a `.env` file with your environment variables
2. Build and run using Docker:
   ```sh
   make docker
   ```

3. Build, scan for security issues, and push to Docker Hub:
   ```sh
   # Full DevSecOps cycle (requires Snyk CLI)
   make docker-devsecops
   
   # Or step by step
   make docker-build
   make docker-security
   make docker-login
   make docker-tag
   make docker-push
   ```

4. For deployment on Fly.io:
   ```sh
   # Deploy to Fly.io
   fly deploy
   ```

### CI/CD with GitHub Actions

We use GitHub Actions for our CI/CD pipeline with strong security controls:

- **Docker Build and Push**: Automatically builds and pushes Docker images to Docker Hub
- **Security Scanning**: 
  - Snyk for container vulnerability scanning
  - GitLeaks for secrets detection
  - SonarCloud for code quality and security analysis
  - OWASP Dependency Check for vulnerable dependencies
- **Automated Testing**: Unit and integration tests run on every push
- **Automated Deployment**: Continuous deployment to Fly.io

See our detailed deployment guides for other platforms:
- [Kubernetes](docs/deployment/KUBERNETES.md)
- [AWS](docs/deployment/AWS.md)
- [Google Cloud](docs/deployment/GCP.md)
- [Azure](docs/deployment/AZURE.md)

## Contributing

We welcome contributions from the community! Please see our [Contributing Guide](CONTRIBUTING.md) for more details on how to get started.

## Code of Conduct

We are committed to fostering an inclusive and welcoming community. Please read and adhere to our [Code of Conduct](CODE_OF_CONDUCT.md).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgements

- [Spring Framework](https://spring.io/)
- [Project Reactor](https://projectreactor.io/)
- [R2DBC](https://r2dbc.io/)
- [Elasticsearch](https://www.elastic.co/)
- [Redis](https://redis.io/)

---

Made with ❤️ by the ChillFlix team