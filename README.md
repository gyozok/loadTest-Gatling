# Load testing project with Gatling

# Order Service — Performance Testing & Observability Pipeline

A small, self-contained microservice project built to practice and demonstrate an end-to-end
performance testing and observability workflow: from load generation, through metrics
collection, to a dashboard that correlates load with system behavior.

## Goal

This project simulates a realistic microservice, 
- generates load against it with Gatling, 
- exposes JVM and application metrics via Micrometer/Prometheus, 
- and  visualizes the results in Grafana — 
so load test results can be directly correlated with system behavior (latency, GC pauses, throughput) rather than viewed in isolation.

Specifically, this project demonstrates:

- Designing and running a load test against a real REST API 
- Integrating performance test results with an existing metrics/observability stack
- Identifying a genuine bottleneck under load and validating a fix with before/after data
- Containerizing and deploying the service, with production-style health checks
- Gating a CI/CD pipeline on performance regressions, not just functional test failures
- Working with event-driven, asynchronous processing via Kafka

## Architecture (Plan)

```
                    ┌──────────────────┐
                    │  Gatling Load    │
                    │  Test            │
                    └────────┬─────────┘
                             │ HTTP
                             ▼
                    ┌──────────────────┐        ┌───────────────────┐
                    │  Order Service   │──────▶ │  Kafka: order-    │
                    │  (Spring Boot)   │        │  events           │
                    └────────┬─────────┘        └──────────┬────────┘
                             │                             │
                             │ /actuator/prometheus        ▼
                             │                   ┌───────────────────┐
                             │                   │  Order Processor  │
                             │                   │  (consumer)       │
                             │                   └───────────────────┘
                             ▼
                    ┌──────────────────┐
                    │  Prometheus      │
                    │  (scrapes both   │
                    │  services)       │
                    └────────┬─────────┘
                             ▼
                    ┌──────────────────┐
                    │  Grafana         │
                    │  Dashboard       │
                    └──────────────────┘
```
Deployment: both services containerized (Docker) and deployed to Kubernetes (readiness/liveness probes, HorizontalPodAutoscaler).
CI/CD: GitHub Actions — build → test → Docker build → Gatling smoke test (fails pipeline on p95 latency regression) → deploy.


## Tech stack

| Layer | Technology                               |
|---|------------------------------------------|
| Service | Java 25, Spring Boot 4                   |
| Messaging | Apache Kafka                             |
| Persistence | H2 (in-memory) via Spring Data JPA       |
| Load testing | Gatling (Java DSL)                       |
| Metrics | Micrometer → Prometheus                  |
| Dashboards | Grafana                                  |
| Containerization | Docker, Docker Compose                   |
| Orchestration | Kubernetes (Minikube/Kind for local dev) |
| CI/CD | GitHub Actions                           |

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/orders` | Creates an order, publishes an `OrderCreated` event to Kafka |
| `GET` | `/orders/{id}` | Retrieves an order by ID |
| `GET` | `/actuator/health` | Health check (used by Docker/K8s probes) |
| `GET` | `/actuator/prometheus` | Metrics endpoint scraped by Prometheus |

## Running locally

```bash
# Start Kafka, Prometheus, Grafana, and both services
docker-compose up --build

# Grafana:    http://localhost:3000  (admin/admin)
# Prometheus: http://localhost:9090
# Order API:  http://localhost:8080
```

## Running the load test

```bash
cd load-tests
mvn gatling:test
```

