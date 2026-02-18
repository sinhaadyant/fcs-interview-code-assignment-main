# Enhancements and advanced features

This document lists **current** and **planned** enhancements beyond the base [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md). Status: **Done** (implemented) or **Planned**.

---

## Multi-lingual (i18n)

| Feature | Status | Notes |
|--------|--------|------|
| **Languages (en, hi, nl)** | Done | `MessageService` + `messages_en.properties`, `messages_hi.properties`, `messages_nl.properties`. Use `Accept-Language: en`, `hi`, or `nl`. |
| **Default locale config** | Done | `app.default-locale` in `application.properties` (default `en`) when header is missing. |
| **Error/success message keys** | Done | All API errors and create success messages use keys; `X-Message` header on 201 for Store/Product create and fulfilment assign. |
| **Documentation** | Done | README, API_VERIFICATION, Postman variable `acceptLanguage`. |

---

## API and contract

| Feature | Status | Notes |
|--------|--------|------|
| **Product-fulfilment HTTP endpoint** | Done | `POST /store/{storeId}/fulfilment` with body `{ "warehouseBusinessUnitCode", "productId" }`. Returns 201, 400, 409. Constraints documented in OpenAPI description. |
| **OpenAPI description for fulfilment** | Done | Warehouse OpenAPI info describes fulfilment path and constraints. |
| **API versioning (header)** | Planned | Optional `X-API-Version` or `Accept` version when needed for backward compatibility. |

---

## Observability and operations

| Feature | Status | Notes |
|--------|--------|------|
| **X-Response-Time-Ms header** | Done | Set on every response by `RequestContextFilter`. |
| **Request size limit** | Done | `quarkus.http.limits.max-body-size=512000` in `application.properties`. |
| **CORS** | Done | `quarkus.http.cors.*` in `application.properties`; exposed headers include `x-request-id`, `x-response-time-ms`, `x-message`. |
| **Metrics (Prometheus)** | Done | `quarkus-micrometer` + `quarkus-micrometer-registry-prometheus`; endpoint `/q/metrics`. |
| **Structured JSON logging** | Done | Dependency `quarkus-logging-json`; enable with `quarkus.log.console.json=true` when needed. |
| **Health checks** | Done | SmallRye Health: `/q/health`, `/q/health/live`, `/q/health/ready`. |

---

## Developer and operator experience

| Feature | Status | Notes |
|--------|--------|------|
| **Docker Compose** | Done | `docker-compose.yml` in java-assignment: app + PostgreSQL; run after `./mvnw package -DskipTests`. |
| **.env.example** | Done | Lists `QUARKUS_*` and `APP_DEFAULT_LOCALE` for local/override. |
| **CI (test/coverage artifact)** | Planned | GitHub Actions could publish test results and JaCoCo report. |

---

## Testing

| Feature | Status | Notes |
|--------|--------|------|
| **Integration test for fulfilment** | Done | `WarehouseEndpointIT.testFulfilmentAssign` (201, idempotent, 400 for invalid warehouse). |
| **Unit tests for common layer** | Done | `MessageServiceTest`, `RequestContextTest`, `GlobalApiExceptionMapperTest` (see `src/test/.../common/`). |
| **Contract test for OpenAPI** | Planned | Assert live app behaviour matches OpenAPI spec. |

---

## Security and resilience

| Feature | Status | Notes |
|--------|--------|------|
| **Rate limiting** | Planned | When needed (e.g. abuse risk). |
| **Request size / CORS** | Done | See Observability. |

---

## Documentation

| Document | Purpose |
|----------|---------|
| [README](README.md) | Features, run guide, APIs, language, Postman. |
| [API_VERIFICATION](API_VERIFICATION.md) | How to verify health, Swagger, Postman, Accept-Language, CRUD. |
| [DOCUMENTATION](DOCUMENTATION.md) | Technical architecture and API summary. |
| [QUESTIONS](QUESTIONS.md) | Design questions and suggested answers. |
| This file | List of enhancements (done and planned). |
