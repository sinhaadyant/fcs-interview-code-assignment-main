# Fulfilment Application — Technical Documentation

**Version:** 1.0.0  
**Audience:** Engineers, QA, Product  
**Status:** Aligned with [CODE_ASSIGNMENT.md](CODE_ASSIGNMENT.md) and [BRIEFING.md](../case-study/BRIEFING.md)

---

## 1. Overview

This document describes the **Warehouse Colocation Management** Java application: its domain, architecture, implementation of the code-assignment tasks, validation rules, API contract, and test strategy. It is written from a senior principal engineer perspective and assumes the reader has read the code assignment and briefing.

### 1.1 Domain Summary

| Entity      | Description |
|------------|-------------|
| **Location** | A geographical place (city/site) with limits: max number of warehouses and max total capacity. |
| **Store**    | A physical store where products are sold; synced to a legacy store management system. |
| **Warehouse**| A place where products are kept before distribution to stores; identified by a unique **Business Unit Code**. |
| **Product**  | Goods sold in stores. |

The system manages creation and lifecycle of Warehouses and Stores, and supports **replacement** of a Warehouse (archive current, create new under same Business Unit Code) and **product fulfilment assignments** (which warehouses fulfil which products for which stores).

---

## 2. Assignment Tasks — Implementation Summary

### 2.1 Task 1: Location — `resolveByIdentifier` (Must Have)

**Package:** `com.fulfilment.application.monolith.location`

**Requirement:** Implement `LocationGateway.resolveByIdentifier(String identifier)` so the warehouse domain can resolve locations by identifier.

**Implementation:**

- **Class:** `LocationGateway` implements `LocationResolver`.
- **Behaviour:**
  - Returns the matching `Location` from a static in-memory catalog when `identifier` is non-null and matches.
  - Returns `null` when `identifier` is `null` or when no location matches (e.g. unknown identifier).
- **Catalog:** Static list of locations (e.g. ZWOLLE-001, AMSTERDAM-001, TILBURG-001) with `identification`, `maxNumberOfWarehouses`, and `maxCapacity`.

**Tests:** `LocationGatewayTest` covers: existing identifier returns correct location; unknown identifier returns null; null identifier returns null (for full branch coverage).

---

### 2.2 Task 2: Store — Legacy Sync After Commit (Must Have)

**Package:** `com.fulfilment.application.monolith.stores`

**Requirement:** Ensure `LegacyStoreManagerGateway` is called only **after** Store changes are **committed to the database**, so the legacy system receives confirmed data.

**Implementation:**

- **Class:** `StoreResource` uses `TransactionSynchronizationRegistry` to register an **interposed synchronization**.
- **Pattern:** For `create`, `update`, and `patch`:
  1. Perform persistence (e.g. `store.persist()` or entity field updates).
  2. Register a `Synchronization` with `transactionSynchronizationRegistry.registerInterposedSynchronization(...)`.
  3. In `afterCompletion(int status)`, only call `legacyStoreManagerGateway.createStoreOnLegacySystem(store)` or `updateStoreOnLegacySystem(entity)` when `status == Status.STATUS_COMMITTED`.
- **Result:** Legacy side effects run only after the JTA transaction has successfully committed; on rollback they are not executed.

**Tests:** `StoreEndpointTest` exercises full CRUD and ensures legacy gateway is invoked after commit (observable via logs/temp file in current simulation). No legacy call on delete (delete does not register a sync).

---

### 2.3 Task 3: Warehouse — Create, Retrieve, Replace, Archive (Must Have)

**Package:** `com.fulfilment.application.monolith.warehouses` (note: CODE_ASSIGNMENT mentions `warehouse` singular; the actual package is **warehouses**).

**Requirement:** Implement API handlers and use cases for creating, retrieving, replacing, and archiving warehouses, with the validations and constraints specified in the assignment.

#### 2.3.1 Business Rules Implemented

**All creation/replacement paths:**

| Rule | Implementation |
|------|----------------|
| **Business Unit Code** | Must be non-null and non-blank. For **create**: must not already exist for an active warehouse. For **replace**: must identify an existing active warehouse. |
| **Location** | Must be provided and must resolve via `LocationResolver.resolveByIdentifier`; otherwise "Location not found". |
| **Capacity** | Must be a positive integer. |
| **Stock** | Must be ≥ 0 and ≤ capacity. |

**Creation only:**

| Rule | Implementation |
|------|----------------|
| **Max warehouses per location** | Count of active warehouses at that location must be &lt; `location.maxNumberOfWarehouses`. |
| **Max capacity per location** | Sum of capacities of active warehouses at that location plus new warehouse capacity must not exceed `location.maxCapacity`. |

**Replacement only:**

| Rule | Implementation |
|------|----------------|
| **Stock matching** | New warehouse `stock` must equal current warehouse `stock`. |
| **Capacity accommodation** | New warehouse `capacity` must be ≥ current warehouse `stock`. |
| **Location limits** | Same max-warehouses and max-capacity rules as creation, applied to the location of the **new** warehouse, counting active warehouses **excluding** the one being replaced. |

**Archive:** Only an active warehouse for the given business unit code can be archived; it is soft-deleted by setting `archivedAt`.

#### 2.3.2 API and Layering

- **Spec:** [warehouse-openapi.yaml](src/main/resources/openapi/warehouse-openapi.yaml) defines the Warehouse API; code is generated into `com.warehouse.api`.
- **REST adapter:** `WarehouseResourceImpl` (implements generated `WarehouseResource`):
  - Maps request beans to domain `Warehouse` and delegates to use cases: `CreateWarehouseOperation`, `ReplaceWarehouseOperation`, `ArchiveWarehouseOperation`.
  - List: uses `WarehouseRepository.findAll()` with pagination (query params `page`, `size`).
  - Get by ID: by database ID; returns 404-equivalent (null) for invalid id, archived warehouse, or parse error.
  - Archive by ID: resolves warehouse by DB id, then calls `ArchiveWarehouseOperation.archive(domainWarehouse)`.
- **Use cases:**  
  - `CreateWarehouseUseCase` — all creation validations and `warehouseStore.create(warehouse)`.  
  - `ReplaceWarehouseUseCase` — replace validations, archive current, then create new with same business unit code.  
  - `ArchiveWarehouseUseCase` — archive validations and `warehouseStore.update(current)` with `archivedAt` set.
- **Persistence:** `WarehouseRepository` implements `WarehouseStore`; uses `DbWarehouse` (JPA) and maps to/from domain `Warehouse`. `findByBusinessUnitCode` considers only active (`archivedAt is null`) records.

**Response behaviour:** Use cases throw `IllegalArgumentException` / `IllegalStateException` for validation failures. The application-wide **GlobalApiExceptionMapper** maps these to **400 Bad Request** and returns the common error format (`code`, `error`, `exceptionType`) so the API aligns with the OpenAPI spec.

---

### 2.4 Bonus: Product Fulfilment Assignments (Nice to Have)

**Package:** `com.fulfilment.application.monolith.warehouses` (domain and adapters)

**Requirement:** Associate Warehouses as fulfilment units for Products for given Stores, with:

1. Max **2** warehouses per product per store.  
2. Max **3** warehouses per store.  
3. Max **5** product types per warehouse.

**Implementation:**

- **Domain:** `ProductFulfilmentAssignment` (storeId, warehouseBusinessUnitCode, productId).  
- **Port:** `AssignWarehouseToProductForStoreOperation.assign(storeId, warehouseBusinessUnitCode, productId)`.  
- **Use case:** `AssignWarehouseToProductForStoreUseCase`:
  - Validates warehouse exists (via `WarehouseStore.findByBusinessUnitCode`).
  - Idempotent: if assignment already exists, returns without error.
  - Enforces: (1) `findByStoreAndProduct` distinct warehouses &lt; 2, (2) `findByStore` distinct warehouses &lt; 3 (or new warehouse already used for that store), (3) `findByWarehouseBusinessUnitCode` distinct products &lt; 5 (or product already assigned).  
- **Persistence:** `ProductFulfilmentRepository` with `DbProductFulfilment`; unique constraint on (storeId, warehouseBusinessUnitCode, productId).

**Note:** The assignment API (HTTP endpoint) is not mandated by the assignment; the use case and port are implemented and tested. An endpoint can be added later that calls `AssignWarehouseToProductForStoreOperation.assign(...)`.

---

## 3. Architecture and Conventions

### 3.1 High-Level Structure

- **Location:** Single adapter `LocationGateway` implementing `LocationResolver` (in-memory catalog).  
- **Stores:** REST `StoreResource`, entity `Store`, `LegacyStoreManagerGateway`; transaction sync for legacy.  
- **Products:** REST `ProductResource`, entity `Product`, `ProductRepository` (Panache).  
- **Warehouses:**  
  - **Domain:** models (`Warehouse`, `Location`, `ProductFulfilmentAssignment`), ports (e.g. `LocationResolver`, `WarehouseStore`, use-case interfaces).  
  - **Use cases:** `CreateWarehouseUseCase`, `ReplaceWarehouseUseCase`, `ArchiveWarehouseUseCase`, `AssignWarehouseToProductForStoreUseCase`.  
  - **Adapters:** REST (`WarehouseResourceImpl`), DB (`WarehouseRepository`, `ProductFulfilmentRepository`, `DbWarehouse`, `DbProductFulfilment`).

### 3.2 Design Decisions

- **Ports and adapters:** Warehouse domain depends only on interfaces (e.g. `LocationResolver`, `WarehouseStore`); infrastructure (REST, JPA) lives in adapters.  
- **Transaction and legacy:** Store updates use JTA and run legacy gateway only after commit via `TransactionSynchronizationRegistry`.  
- **Warehouse API:** OpenAPI-first for Warehouse; Product and Store are hand-written JAX-RS resources.  
- **Persistence:** Quarkus Hibernate ORM with Panache; PostgreSQL in production profile; dev/test use in-memory or test DB with `import.sql` for seed data.

### 3.3 Database Access Patterns

- **Store / Product:** Entity extends PanacheEntity; repository extends PanacheRepository; some logic in resource.  
- **Warehouse:** Separate domain model and JPA entity (`DbWarehouse`); repository implements domain port and maps between them.  
- **Product fulfilment:** Same pattern: `DbProductFulfilment` and `ProductFulfilmentRepository` implementing `ProductFulfilmentStore`.

Refactoring could unify one of these styles (e.g. all via ports with explicit mapping) for consistency; see [QUESTIONS.md](QUESTIONS.md) Q1.

---

## 4. Common API Response Format

All error responses (4xx and 5xx) use the same JSON structure across **Store**, **Product**, and **Warehouse** APIs so that clients can handle errors consistently.

**Error response body:**

```json
{
  "code": 400,
  "error": "Human-readable message",
  "exceptionType": "java.lang.IllegalArgumentException"
}
```

- **code** — HTTP status code (e.g. 400, 404, 422, 500).  
- **error** — Short message describing the failure.  
- **exceptionType** — Optional; exception class name for debugging.

A single **GlobalApiExceptionMapper** (`com.fulfilment.application.monolith.common`) builds this shape for every unhandled exception: `WebApplicationException` uses its response status; `IllegalArgumentException` and `IllegalStateException` map to **400 Bad Request**; all others to **500**.

---

## 5. API Reference (Summary)

### 5.1 Store — `GET/POST/PUT/PATCH/DELETE /store`, `/store/{id}`

- List: pagination `page`, `size`.  
- Create: body `Store` (name, quantityProductsInStock); 201 on success; legacy sync after commit.  
- Update/Patch: 404 if store not found; 422 if name not set; legacy sync after commit.  
- Delete: 204; no legacy sync.

### 5.2 Product — `GET/POST/PUT/DELETE /product`, `/product/{id}`

- List: pagination `page`, `size`.  
- Create: body `Product`; 201; validation on entity.  
- Update: 404 if not found; 422 if name not set.  
- Delete: 204.

### 5.3 Warehouse — OpenAPI spec: `src/main/resources/openapi/warehouse-openapi.yaml`

- **GET /warehouse** — List **active (non-archived)** warehouses only; paginated via query params `page`, `size`. Response items include **id** (DB id), businessUnitCode, location, capacity, stock.
- **POST /warehouse** — Create; body: businessUnitCode, location, capacity, stock; 201/400. Response includes **id**.
- **GET /warehouse/{id}** — Get by DB id; 200 with body (including **id**) when found. **404** when warehouse does not exist, is archived, or id is invalid/non-numeric (common error format).
- **DELETE /warehouse/{id}** — Archive by DB id; **204** when archived. **404** when warehouse not found or already archived (common error format).
- **POST /warehouse/{businessUnitCode}/replacement** — Replace active warehouse; body: location, capacity, stock (businessUnitCode from path); 200/400/404. Response includes **id**.

---

## 6. Test Strategy and Coverage

### 6.1 Goals

- **Correctness:** All assignment requirements are implemented and validated.  
- **100% line and branch coverage** for assignment-relevant code (excluding generated OpenAPI beans and optional third-party code).  
- **Layered testing:** Unit tests for use cases and gateways; integration/endpoint tests for REST and DB.

### 6.2 Test Types

| Layer | Tool | Scope |
|-------|------|--------|
| **Unit** | JUnit 5 | `LocationGateway`, `CreateWarehouseUseCase`, `ReplaceWarehouseUseCase`, `ArchiveWarehouseUseCase`, `AssignWarehouseToProductForStoreUseCase` (with in-memory stubs for ports). |
| **REST (Quarkus)** | JUnit 5 + RestAssured + `@QuarkusTest` | Store, Product, Warehouse endpoints (CRUD, validation, 404/422, legacy behaviour where applicable). |
| **Integration** | `@QuarkusIntegrationTest` | Full stack Warehouse flows (e.g. list, archive, replace) in packaged application. |

### 6.3 Coverage Enforcement

- **JaCoCo** is configured in the build (`jacoco-maven-plugin`): prepare-agent, report after tests, with exclusions for generated API beans (`com/warehouse/api/beans/*`).  
- **Target:** 100% line and branch coverage for assignment-critical code:  
  - **Location:** `LocationGateway` — 100% (unit tests).  
  - **Warehouse domain:** use cases and models — high coverage via unit tests; all validation branches and edge cases covered.  
  - **Stores, Products, Warehouses:** REST and DB adapters are exercised by `@QuarkusTest` and `@QuarkusIntegrationTest`; depending on test fork/classloader behaviour, adapter coverage may appear in a separate JaCoCo session or still contribute to overall quality.  
- Unit tests cover every validation branch and null/blank inputs in use cases and `LocationGateway`.  
- REST tests (`StoreEndpointTest`, `ProductEndpointTest`, `WarehouseEndpointTest`) cover success and failure paths (404, 422, invalid id, validation errors).

### 6.4 Test List (Summary)

- **LocationGatewayTest:** resolve existing, unknown, null.  
- **StoreEndpointTest:** full CRUD; legacy called after commit (create/update/patch).  
- **ProductEndpointTest:** full CRUD.  
- **CreateWarehouseUseCaseTest:** valid create; duplicate BU; unknown location; max warehouses at location; capacity exceeds location max; null/blank/incorrect capacity/stock/location.  
- **ReplaceWarehouseUseCaseTest:** valid replace; no current warehouse; stock mismatch; capacity &lt; stock; location not found; location at capacity/warehouse limit.  
- **ArchiveWarehouseUseCaseTest:** archive existing; no active warehouse; null/blank BU.  
- **AssignWarehouseToProductForStoreUseCaseTest:** valid assign; &gt;2 warehouses per product/store; &gt;3 warehouses per store; &gt;5 products per warehouse; warehouse not found; null params; idempotent.  
- **WarehouseEndpointTest** (`@QuarkusTest`): list, create, get by id, archive, replace; invalid id, validation errors (400), 404 behaviour.  
- **WarehouseEndpointIT** (`@QuarkusIntegrationTest`): end-to-end warehouse flows.

---

## 7. Requirements Compliance Matrix

| Requirement | Implementation | Verified By |
|-------------|----------------|------------|
| Location `resolveByIdentifier` | `LocationGateway` returns location or null | LocationGatewayTest |
| Legacy sync only after commit | `TransactionSynchronizationRegistry` + `afterCompletion(COMMITTED)` | StoreEndpointTest + design |
| Warehouse create with all validations | CreateWarehouseUseCase + WarehouseResourceImpl | CreateWarehouseUseCaseTest, WarehouseEndpointTest |
| Warehouse retrieve | GET by id in WarehouseResourceImpl | WarehouseEndpointTest |
| Warehouse replace with stock/capacity/location rules | ReplaceWarehouseUseCase + REST | ReplaceWarehouseUseCaseTest, WarehouseEndpointTest |
| Warehouse archive | ArchiveWarehouseUseCase + DELETE by id | ArchiveWarehouseUseCaseTest, WarehouseEndpointTest |
| BU code uniqueness (create) | CreateWarehouseUseCase | CreateWarehouseUseCaseTest |
| Location validation | All use cases via LocationResolver | Use case tests |
| Max warehouses/capacity per location | CreateWarehouseUseCase, ReplaceWarehouseUseCase | Use case tests |
| Stock ≤ capacity; replace stock match & capacity ≥ stock | Create + Replace use cases | Use case tests |
| Bonus: fulfilment constraints (2/3/5) | AssignWarehouseToProductForStoreUseCase | AssignWarehouseToProductForStoreUseCaseTest |
| 100% test coverage (assignment code) | Full unit + REST + IT tests | JaCoCo report |

---

## 8. How to Build and Run

- **Build and unit tests:** `./mvnw clean test`  
- **Coverage report:** `target/site/jacoco/index.html` (after `./mvnw test`)  
- **Run application:** `./mvnw quarkus:dev` (dev), or `./mvnw package` then `java -jar target/quarkus-app/quarkus-run.jar` (with PostgreSQL).  
- **Integration tests:** `./mvnw verify` (includes `@QuarkusIntegrationTest` when configured).

---

## 9. References

- [CODE_ASSIGNMENT.md](CODE_ASSIGNMENT.md) — Task list and validations.  
- [BRIEFING.md](../case-study/BRIEFING.md) — Domain and context (path may vary by repo layout).  
- [QUESTIONS.md](QUESTIONS.md) — Design and testing questions (with suggested answers).  
- [ARCHITECTURE_REVIEW_AND_IMPROVEMENTS.md](ARCHITECTURE_REVIEW_AND_IMPROVEMENTS.md) — Senior architect review, gaps, and improvement suggestions.  
- [warehouse-openapi.yaml](src/main/resources/openapi/warehouse-openapi.yaml) — Warehouse API contract.
