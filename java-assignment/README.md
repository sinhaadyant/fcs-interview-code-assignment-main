# Java Code Assignment — Fulfilment Application

A Quarkus-based REST API for managing **Stores**, **Products**, and **Warehouses**, with location validation, legacy sync guarantees, and optional product-fulfilment assignments. This README describes every feature, how to run the project end-to-end, and **what was added beyond** the base [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md).

---

## Table of contents

- [Assignment reference](#assignment-reference)
- [Features implemented](#features-implemented)
- [How to run everything](#how-to-run-everything)
- [APIs and tooling](#apis-and-tooling)
- [What was added compared to CODE_ASSIGNMENT](#what-was-added-compared-to-code_assignmentmd)
- [Project structure](#project-structure)
- [Documentation index](#documentation-index)
- [Troubleshooting](#troubleshooting)
- [Push to GitHub and share the link](#push-to-github-and-share-the-link)

---

## Assignment reference

The base tasks are defined in **[CODE_ASSIGNMENT.md](CODE_ASSIGNMENT.md)**:

| Task | Type | Description |
|------|------|-------------|
| **1. Location** | Must have | Implement `LocationGateway.resolveByIdentifier` so warehouses can resolve locations by identifier. |
| **2. Store** | Must have | Ensure `LegacyStoreManagerGateway` is called **only after** Store changes are **committed to the database**. |
| **3. Warehouse** | Must have | Implement Warehouse **create**, **retrieve**, **replace**, and **archive** with validations: business unit uniqueness, location validity, max warehouses/capacity per location, capacity/stock rules; for replace: stock matching and capacity accommodation. |
| **4. Product fulfilment** | Bonus | Associate warehouses as fulfilment units for products per store (max 2 warehouses per product per store, max 3 warehouses per store, max 5 products per warehouse). |

**Prerequisites (from assignment):** JDK 17+; PostgreSQL or Docker for DB. Read the [brief overview](../case-study/BRIEFING.md) for domain and business rules.

---

## Features implemented

### Assignment tasks (from CODE_ASSIGNMENT.md)

- **Location** — `LocationGateway.resolveByIdentifier(String)` implemented; in-memory catalog of locations (e.g. ZWOLLE-001, AMSTERDAM-001) with `maxNumberOfWarehouses` and `maxCapacity`. Used by warehouse use cases for location validation.
- **Store** — `StoreResource` uses `TransactionSynchronizationRegistry` to run `LegacyStoreManagerGateway` (create/update) **only after** JTA commit. Create, update, and patch register an interposed synchronization; legacy is not called on rollback.
- **Warehouse** — Full API and use cases:
  - **Create:** Business unit code uniqueness, location exists, max warehouses per location, max capacity per location, capacity/stock validation.
  - **Replace:** Stock must match current warehouse; new capacity must accommodate stock; location limits enforced (current warehouse excluded from count).
  - **Archive:** Soft-delete by `archivedAt`; list and get-by-id return only **active** warehouses.
  - **List/Get:** Pagination (`page`, `size`); responses include `id` (DB id).
- **Bonus — Product fulfilment** — `AssignWarehouseToProductForStoreUseCase` and `AssignWarehouseToProductForStoreOperation` with constraints: max 2 warehouses per product per store, max 3 warehouses per store, max 5 products per warehouse. Persistence via `ProductFulfilmentRepository`; use case is tested (HTTP endpoint for assign is optional and can be added later).

### Store and Product APIs (existing; aligned)

- **Store:** `GET/POST/PUT/PATCH/DELETE` on `/store` and `/store/{id}`; pagination; 201 on create; 404/422 where specified; legacy sync after commit for create/update/patch.
- **Product:** `GET/POST/PUT/DELETE` on `/product` and `/product/{id}`; pagination; 201 on create; 404/422 where specified.

### Cross-cutting (additional to assignment)

- **Structured error responses** — All 4xx/5xx use a common JSON shape: `timestamp`, `status`, `error`, `message`, `path`, `errorCode`, `traceId`, optional `details[]`. Handled by `GlobalApiExceptionMapper`; no stack traces in responses.
- **Request tracing** — `RequestContextFilter` generates or reads `X-Request-Id`, stores requestId/path/language in `RequestContext`; every error response includes `traceId`; completion logged with `requestId`, `path`, `status`, `processingTimeMs`.
- **i18n (en/hi)** — `MessageService` and `messages_en.properties` / `messages_hi.properties`; `Accept-Language` drives language; error/success messages externalized.
- **Health checks** — SmallRye Health: `/q/health`, `/q/health/live`, `/q/health/ready` (readiness includes DB).
- **Swagger UI & OpenAPI** — SmallRye OpenAPI: `/q/swagger-ui`, `/q/openapi` (YAML); Store, Product, and Warehouse endpoints discoverable.
- **Postman collection** — `postman/Java-Assignment-API.postman_collection.json` with Store, Product, and Warehouse requests; variable `baseUrl` (default `http://localhost:8080`).
- **CI/CD** — GitHub Actions workflow: build, test, package on push/PR to `main` or `master`; JAR artifact uploaded.

---

## How to run everything

All commands below are from the **java-assignment** directory (project root for this app).

### 1. Prerequisites

- **JDK 17+** — `JAVA_HOME` set; `java -version` shows 17 or higher.
- **Maven** — Wrapper included: use `./mvnw` (Unix) or `mvnw.cmd` (Windows).
- **Database (for production-style run):** PostgreSQL or Docker. Dev/test can use Quarkus Dev Services (PostgreSQL container started automatically).

### 2. Build

```bash
./mvnw clean package
```

- Compiles main and test code, runs **all tests**, generates OpenAPI-based Warehouse resource, produces JAR under `target/`.
- To skip tests: `./mvnw clean package -DskipTests`.

### 3. Run tests only

```bash
./mvnw clean test
```

- Runs **58 tests** (Store, Product, Warehouse, Location, use cases); JaCoCo coverage applied (e.g. 40% line minimum, excluding generated OpenAPI code).
- Ensure no other app is bound to the port used by `@QuarkusTest` (tests start an in-memory/test DB and app context).

### 4. Run the application (development)

```bash
./mvnw quarkus:dev
```

- Starts the app with **live reload**; Quarkus can start a PostgreSQL container via Dev Services if no DB is configured.
- Base URL: **http://localhost:8080**
- Stop with `Ctrl+C`.

### 5. Run the application (production-style JAR)

**5.1 Start PostgreSQL (if not using Dev Services)**

Example with Docker:

```bash
docker run -it --rm --name quarkus_test \
  -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test \
  -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 \
  postgres:13.3
```

Connection settings for **production profile** are in `src/main/resources/application.properties` (`%prod.*`).

**5.2 Build and run**

```bash
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

Or with production profile:

```bash
java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar
```

### 6. Verify after startup

- **Web:** http://localhost:8080/index.html  
- **Swagger UI:** http://localhost:8080/q/swagger-ui  
- **OpenAPI:** http://localhost:8080/q/openapi  
- **Health:** http://localhost:8080/q/health (combined), `/q/health/live`, `/q/health/ready`  
- **APIs:** e.g. `curl http://localhost:8080/store`, `curl http://localhost:8080/product`, `curl http://localhost:8080/warehouse`

---

## APIs and tooling

| Resource | Base path | Operations | Pagination |
|----------|-----------|------------|------------|
| **Store** | `/store` | GET (list), GET `/{id}`, POST, PUT `/{id}`, PATCH `/{id}`, DELETE `/{id}` | `page`, `size` |
| **Product** | `/product` | GET (list), GET `/{id}`, POST, PUT `/{id}`, DELETE `/{id}` | `page`, `size` |
| **Warehouse** | `/warehouse` | GET (list), GET `/{id}`, POST, DELETE `/{id}` (archive), POST `/{businessUnitCode}/replacement` | `page`, `size` |

- **Error format:** All errors return a common JSON body (`status`, `message`, `traceId`, `path`, `errorCode`, etc.); see [DOCUMENTATION](DOCUMENTATION.md).
- **Swagger UI:** http://localhost:8080/q/swagger-ui — try all endpoints.
- **Postman:** Import `postman/Java-Assignment-API.postman_collection.json`; set `baseUrl` to `http://localhost:8080` (or your server).
- **Health:** Use `/q/health`, `/q/health/live`, `/q/health/ready` for probes (e.g. Kubernetes, load balancers).

---

## What was added compared to CODE_ASSIGNMENT.md

The assignment asks for: **Location**, **Store (legacy after commit)**, **Warehouse (create/replace/archive + validations)**, and **Bonus (product fulfilment)**. The following were **added on top** of that:

| Addition | Description |
|----------|-------------|
| **Structured error API** | Single error JSON shape for all APIs (`StructuredErrorResponse`: timestamp, status, error, message, path, errorCode, traceId, details). Implemented in `GlobalApiExceptionMapper`; no stack traces in responses. |
| **Request context & tracing** | `RequestContextFilter` + `RequestContext`: generate/read `X-Request-Id`, store path and language; log request completion with status and processing time; include `traceId` in every error response. |
| **i18n (en/hi)** | `MessageService` and `messages_en.properties` / `messages_hi.properties`; user-facing strings externalized; `Accept-Language` for locale. |
| **Health checks** | SmallRye Health: `/q/health`, `/q/health/live`, `/q/health/ready` (readiness includes DB). Dependency: `quarkus-smallrye-health`. |
| **Swagger UI & OpenAPI** | SmallRye OpenAPI: `/q/swagger-ui`, `/q/openapi`. Dependency: `quarkus-smallrye-openapi`. |
| **Postman collection** | Ready-to-import collection for Store, Product, and Warehouse in `postman/Java-Assignment-API.postman_collection.json`. |
| **CI/CD** | GitHub Actions workflow under `.github/workflows/java-assignment-ci.yml`: on push/PR to `main` or `master`, run build + test + package; upload JAR artifact. |
| **Documentation** | [DOCUMENTATION](DOCUMENTATION.md), [ARCHITECTURE_REVIEW_AND_IMPROVEMENTS](ARCHITECTURE_REVIEW_AND_IMPROVEMENTS.md), [PRODUCTION_IMPROVEMENTS_REPORT](PRODUCTION_IMPROVEMENTS_REPORT.md), [API_VERIFICATION](API_VERIFICATION.md), [QUESTIONS](QUESTIONS.md). |

The **core assignment scope** (Location, Store legacy sync, Warehouse CRUD + validations, bonus fulfilment) is implemented and tested; the items above improve operability, consistency, and documentation.

---

## Project structure

```
java-assignment/
├── src/main/java/com/fulfilment/application/monolith/
│   ├── common/           # GlobalApiExceptionMapper, RequestContext, RequestContextFilter, MessageService, StructuredErrorResponse, ErrorDetail
│   ├── location/         # LocationGateway (LocationResolver)
│   ├── products/         # Product, ProductRepository, ProductResource
│   ├── stores/           # Store, StoreResource, LegacyStoreManagerGateway
│   └── warehouses/
│       ├── adapters/     # restapi (WarehouseResourceImpl), database (DbWarehouse, WarehouseRepository, ProductFulfilmentRepository)
│       └── domain/       # models, ports, usecases (Create, Replace, Archive, Assign)
├── src/main/resources/
│   ├── application.properties
│   ├── import.sql        # Seed data (store, product, warehouse)
│   ├── messages_en.properties, messages_hi.properties
│   ├── openapi/warehouse-openapi.yaml
│   └── META-INF/resources/index.html
├── src/test/java/        # StoreEndpointTest, ProductEndpointTest, WarehouseEndpointTest, WarehouseEndpointIT, LocationGatewayTest, use-case tests
├── postman/              # Java-Assignment-API.postman_collection.json
├── pom.xml
├── CODE_ASSIGNMENT.md    # Base assignment tasks
├── DOCUMENTATION.md      # Technical documentation
├── README.md             # This file
└── ...
```

---

## Documentation index

| Document | Purpose |
|----------|---------|
| [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) | Original assignment tasks (Location, Store, Warehouse, Bonus). |
| [DOCUMENTATION](DOCUMENTATION.md) | Technical description, architecture, API summary, test strategy. |
| [ARCHITECTURE_REVIEW_AND_IMPROVEMENTS](ARCHITECTURE_REVIEW_AND_IMPROVEMENTS.md) | Senior architect review, gaps, improvement suggestions. |
| [PRODUCTION_IMPROVEMENTS_REPORT](PRODUCTION_IMPROVEMENTS_REPORT.md) | Production-style changes (errors, i18n, logging, health, etc.). |
| [API_VERIFICATION](API_VERIFICATION.md) | How to verify APIs, health, Swagger, Postman, CRUD. |
| [QUESTIONS](QUESTIONS.md) | Design and improvement questions (with suggested answers). |

---

## Troubleshooting

- **Compilation / generated code:** If your IDE does not see the generated Warehouse API, add `target/generated-sources/jaxrs` (or `target/.../jaxrs`) as generated sources.
- **Port in use:** Change `quarkus.http.port` in `application.properties` if 8080 is taken.
- **Database:** For `java -jar` run, ensure PostgreSQL is reachable with the `%prod` settings in `application.properties` (or use profile and correct URL).
- **Tests fail:** Run `./mvnw clean test`; ensure no other process is using the test port. Dev Services will start a temporary PostgreSQL for tests if needed.

---

## Push to GitHub and share the link

1. **Create a new repository** on GitHub (e.g. `fcs-java-assignment`).

2. **From the repository root** (the folder that contains `java-assignment` and `.github`):

   ```bash
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
   git add .
   git commit -m "Java assignment: APIs, CI/CD, health checks, Postman, Swagger, docs"
   git branch -M main
   git push -u origin main
   ```

   If the repo already exists and you only need to push updates:

   ```bash
   git add .
   git commit -m "Update: in-depth README, features, run guide"
   git push origin main
   ```

3. **Share the link:**  
   **Repository:** `https://github.com/YOUR_USERNAME/YOUR_REPO`  
   (Replace `YOUR_USERNAME` and `YOUR_REPO` with your GitHub username and repository name.)

---

**Summary:** This project implements all [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) tasks (Location, Store legacy-after-commit, Warehouse create/replace/archive with validations, and bonus product fulfilment), plus structured errors, request tracing, i18n, health checks, Swagger UI, Postman collection, and CI/CD. Use the steps in [How to run everything](#how-to-run-everything) to build, test, and run the application.
