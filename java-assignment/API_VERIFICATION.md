# API & Tooling Verification Report

Last verified: run `./mvnw quarkus:dev` then use the steps below (or run the test suite).

## 1. Automated tests

```bash
./mvnw clean test
```

- **58 tests** (Store, Product, Warehouse, Location, use cases) must pass.
- All CRUD and error paths are covered by `StoreEndpointTest`, `ProductEndpointTest`, `WarehouseEndpointTest`, and use-case tests.

## 2. Health checks

With the app running:

| Endpoint | Purpose |
|----------|---------|
| GET /q/health | Combined status (JSON) |
| GET /q/health/live | Liveness (e.g. Kubernetes) |
| GET /q/health/ready | Readiness (includes DB) |

## 3. Swagger UI & OpenAPI

With the app running (`./mvnw quarkus:dev`):

| What | URL | Expected |
|------|-----|----------|
| Swagger UI | http://localhost:8080/q/swagger-ui | 200, interactive UI |
| OpenAPI spec | http://localhost:8080/q/openapi | 200, YAML with all paths |

**Paths in OpenAPI:** `/store`, `/store/{id}`, `/product`, `/product/{id}`, `/warehouse`, `/warehouse/{id}`, `/warehouse/{businessUnitCode}/replacement`.

## 4. Postman

- **Collection:** `postman/Java-Assignment-API.postman_collection.json`
- **Import in Postman:** File → Import → select the collection file.
- **Variable:** `baseUrl` = `http://localhost:8080` (change if your server runs elsewhere).

**Requests in collection:**

| Folder | Operations |
|--------|------------|
| Store | List (GET), Get by id (GET), Create (POST), Update (PUT), Patch (PATCH), Delete (DELETE) |
| Product | List (GET), Get by id (GET), Create (POST), Update (PUT), Delete (DELETE) |
| Warehouse | List (GET), Get by id (GET), Create (POST), Replace (POST .../replacement), Archive (DELETE) |

## 5. CRUD verification (manual / curl)

With app running on port 8080:

- **Store:** GET /store → 200, GET /store/1 → 200, POST /store (JSON body) → 201, PUT /store/1 → 200, PATCH /store/1 → 200, DELETE /store/1 → 204, GET /store/99999 → 404.
- **Product:** GET /product → 200, GET /product/1 → 200, POST /product → 201, PUT /product/1 → 200, DELETE /product/1 → 204, GET /product/99999 → 404.
- **Warehouse:** GET /warehouse → 200, GET /warehouse/1 → 200, POST /warehouse → 200/201, POST /warehouse/MWH.012/replacement → 200, DELETE /warehouse/3 → 204, GET /warehouse/3 after archive → 404, GET /warehouse/99999 → 404.

All 404 responses use the common error format (e.g. `status`, `message`, `traceId`, `path`).
