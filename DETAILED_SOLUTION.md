## Overview

This repository contains a Java/Quarkus monolith that models a simplified **warehouse colocation management system**. In this write‑up I describe:

- **What the system does** and how the main entities (`Location`, `Store`, `Warehouse`, `Product`) relate to each other.
- **How I approached the assignment tasks**, including the trade‑offs behind the main design decisions.
- **How to run, test, and exercise the API**, including CI, GitHub setup, and a ready‑to‑use Postman collection.

---

## Domain and Architecture Overview

At a high level the system is a small monolith with a fairly clean separation between HTTP, domain logic, and persistence:

- **Location**
  - Represents a **city/area** where warehouses can exist.
  - Implemented as an in‑memory catalog in `LocationGateway`, which fulfils the `LocationResolver` domain port.
  - Each `Location` carries:
    - An identifier such as `"ZWOLLE-001"`.
    - The maximum number of warehouses allowed in that area.
    - The maximum total capacity for that area, used when validating warehouse creation/replacement.

- **Store**
  - Represents a **physical store** that sells products.
  - Implemented as a Panache entity (`Store`) with a JAX‑RS layer (`StoreResource`).
  - Every change is also mirrored to a **legacy system** via `LegacyStoreManagerGateway`; this sync must only happen after the database transaction commits successfully.

- **Warehouse**
  - Represents a warehouse where products are stored before being shipped to stores.
  - Identified by a `businessUnitCode` and carrying `location`, `capacity`, `stock`, timestamps and an archived flag.
  - Exposed through code generated from the OpenAPI spec:
    - Interface: `com.warehouse.api.WarehouseResource`
    - Implementation: `WarehouseResourceImpl`
  - The domain logic for warehouses lives in the `warehouses.domain` package, with persistence handled by `WarehouseRepository`.
  - Warehouses support a special **“replace”** operation: the current warehouse for a business unit is archived and a new one is created under the same `businessUnitCode`, with a set of business rules around stock and capacity.

- **Product**
  - Represents SKUs that can be stored in warehouses and sold in stores.
  - The bonus task extends the model with fulfilment relationships between `Products`, `Stores`, and `Warehouses` under a couple of global constraints (max warehouses per product/store, and max products per warehouse).

---

## Task Analysis in Depth

### 1. Location – Implement `LocationGateway.resolveByIdentifier` (Must have)

This is the smallest piece of the exercise and it feeds into all warehouse validations.

- The gateway keeps a static `List<Location>` with a handful of predefined locations.
- `LocationGateway` implements the `LocationResolver` port, which the warehouse use cases rely on to:
  - Check that a requested location actually exists.
  - Evaluate per‑location limits such as number of warehouses and total capacity.

What I implemented:

- `resolveByIdentifier(String identifier)` does a simple stream lookup over the in‑memory list:
  - Returns the matching `Location` when the identifier exists.
  - Returns `null` when there is no match, which the use cases then treat as “location not found”.
- For this scale a linear search is more than enough; if locations ever moved to a database or grew larger, the implementation can be swapped without touching the domain code because it is behind the `LocationResolver` interface.

---

### 2. Store – Call the legacy system only after commit (Must have)

The store API already works functionally, but it has a subtle consistency problem: it calls the legacy integration regardless of whether the database transaction eventually commits.

- `StoreResource` methods (`create`, `update`, `patch`) are marked `@Transactional` and:
  - Persist or update entities through Panache.
  - Immediately call `LegacyStoreManagerGateway`.
- If anything fails after the legacy call but before commit, the DB changes roll back while the legacy system keeps the “successful” update. That is exactly what we want to avoid.

What I changed:

- Injected `TransactionSynchronizationRegistry` into `StoreResource`.
- In `create`, `update` and `patch`:
  - I perform the database work as before.
  - Then I register an interposed `Synchronization` that runs in `afterCompletion`.
  - Inside `afterCompletion` I check the status and only invoke the legacy gateway when `status == STATUS_COMMITTED`.
- For `patch` I also tightened the update logic:
  - Only overwrite the name if it was provided in the payload.
  - Only overwrite the quantity when the incoming value is non‑zero.

This keeps the implementation simple, leverages the transaction manager Quarkus already uses, and gives us the guarantee that the legacy system will only see committed state.

---

### 3. Warehouse – Create, fetch, archive and replace (Must have)

The warehouse part is where most of the interesting business logic lives. The stack looks like this:

- API interface generated from OpenAPI: `com.warehouse.api.WarehouseResource`.
- Concrete JAX‑RS implementation: `WarehouseResourceImpl`.
- Domain model and ports:
  - `warehouses.domain.models.Warehouse`
  - Use cases under `warehouses.domain.usecases.*`
  - Ports in `warehouses.domain.ports.*`
  - Persistence adapter: `warehouses.adapters.database.WarehouseRepository`.

#### Create a warehouse

For creation I wanted all rules in one place (the use case), with the REST layer doing only mapping and HTTP concerns.

- The REST layer:
  - Accepts a `com.warehouse.api.beans.Warehouse`.
  - Maps it into the domain `Warehouse` model.
  - Delegates to `CreateWarehouseUseCase`.

- The use case performs the following checks before persisting:
  - The request is well‑formed:
    - `businessUnitCode` is present and non‑blank.
    - `capacity` is positive.
    - `stock` is not negative and does not exceed `capacity`.
  - The business unit code is unique among **active** warehouses:
    - Uses `WarehouseStore.findByBusinessUnitCode` to ensure there is no active record with the same code.
  - The location is valid:
    - Uses `LocationResolver` to resolve the location identifier.
    - Fails fast if the location does not exist.
  - Per‑location limits:
    - Counts active warehouses at that location and ensures we do not exceed `maxNumberOfWarehouses`.
    - Sums the capacities of active warehouses at that location and ensures `currentSum + newCapacity` does not exceed `maxCapacity`.

If all checks pass, the use case calls `warehouseStore.create(warehouse)`, which the repository implements using the `DbWarehouse` JPA entity.

#### Retrieve a warehouse

- The generated interface models `GET /warehouse/{id}` as a lookup by numeric ID.
- `WarehouseResourceImpl.getAWarehouseUnitByID`:
  - Parses the `id` path parameter as a `Long`.
  - Fetches the underlying `DbWarehouse` by JPA ID.
  - Returns `null` when:
    - The ID cannot be parsed, or
    - The warehouse does not exist, or
    - The warehouse is already archived.
  - Otherwise maps the entity back to the API `Warehouse` bean and returns it.

This keeps the ID‑based lookup straightforward and hides the internal `archivedAt` handling from the client.

#### Archive a warehouse

- `DELETE /warehouse/{id}` is implemented by:
  - Looking up the `DbWarehouse` by ID.
  - Ignoring the call if the record does not exist or is already archived (idempotent behaviour).
  - Converting the entity to the domain model and passing it to `ArchiveWarehouseUseCase`.

- The use case:
  - Re‑resolves the current active warehouse by `businessUnitCode` (to avoid acting on stale state).
  - Fails if there is no active warehouse for that code.
  - Sets `archivedAt` to `now` and delegates to `warehouseStore.update`.

This gives a single path that is responsible for archiving a warehouse, whether it is triggered by a direct archive call or as part of a replace operation.

#### Replace a warehouse

Replacing a warehouse is essentially a composite operation: validate a new warehouse, ensure it can take over the current stock, archive the existing one, and create the new one under the same business unit code.

- `POST /warehouse/{businessUnitCode}/replacement`:
  - Maps the body to a domain `Warehouse`.
  - Overwrites its `businessUnitCode` with the one from the path.
  - Delegates to `ReplaceWarehouseUseCase`.

- The use case performs:
  - Basic validations on the new warehouse (same as create: required fields, capacity and stock sanity).
  - Looks up the current active warehouse for that `businessUnitCode`.
  - Enforces replacement‑specific rules:
    - **Stock must match**: the new warehouse’s `stock` must equal the current warehouse’s `stock`.
    - **Capacity must fit**: the new warehouse’s `capacity` must be at least as large as the current stock.
  - Validates the target location, again via `LocationResolver`, and applies the same per‑location limits as in creation, but:
    - Excludes the current warehouse when counting and summing capacity, since it is being archived in the same operation.
  - Archives the current warehouse by marking `archivedAt` and updating it through the store.
  - Sets timestamps on the new warehouse and calls `warehouseStore.create` to persist it as the new active record for that business unit.

With this setup the REST layer is very thin and all the business rules are in the use cases, which makes them easier to test in isolation.

---

### 4. Bonus – Product/Store/Warehouse fulfilment

If I had time for the bonus, I would model fulfilment explicitly instead of trying to overload existing entities with more fields.

- The goal is to express that:
  1. Each `Product` can be fulfilled by **at most 2 warehouses per store**.
  2. Each `Store` can be fulfilled by **at most 3 warehouses** in total.
  3. Each `Warehouse` can hold **at most 5 different product types**.

The simplest way to capture this is a dedicated association entity, for example `ProductFulfilment`:

- Fields:
  - `storeId`
  - `warehouseId`
  - `productId`
  - Optional knobs like priority or lead time if needed later.
- Natural constraints:
  - The triple `(storeId, warehouseId, productId)` should be unique.
  - `(storeId, productId)` should never end up with more than two distinct warehouses.
  - A given `storeId` should reference at most three distinct `warehouseId`s.
  - A given `warehouseId` should reference at most five distinct `productId`s.

I would enforce this in two layers:

- In the **domain layer**:
  - Introduce a use case like `AssignWarehouseToProductForStore`.
  - When creating a new assignment, query:
    - How many warehouses already serve `(store, product)`.
    - How many distinct warehouses already serve the store.
    - How many distinct products are already served by the warehouse.
  - Reject the operation with a clear domain error as soon as any limit would be exceeded.

- In the **persistence layer**:
  - Add a unique constraint on `(storeId, warehouseId, productId)` to prevent duplicates.
  - Optionally add database‑level checks for some of the counts if this ever becomes a bottleneck; to start with, I’d keep the smarter logic in the domain where it is easier to test and evolve.

---

## Testing Strategy

I treat the warehouse logic as the core of the system and would build the test suite around that, layering outwards.

- **What’s already there**
  - `LocationGatewayTest` for basic location resolution.
  - Skeletons for `CreateWarehouseUseCaseTest`, `ReplaceWarehouseUseCaseTest`, and `ArchiveWarehouseUseCaseTest`.
  - `WarehouseEndpointIT` as an end‑to‑end test on top of HTTP and the database.

- **Where I would invest first**
  - **Unit tests for the warehouse use cases**
    - Cover happy paths and each business rule separately:
      - Duplicate business unit.
      - Unknown location.
      - Too many warehouses at a location.
      - Capacity too small for the requested stock.
      - Replacement rules (capacity accommodation and stock matching).
    - These are cheap to run and give very targeted feedback when a rule is broken.
  - **Repository‑level tests**
    - Exercise `WarehouseRepository` directly to confirm:
      - Active vs archived semantics in `findByBusinessUnitCode`.
      - How `getAll` behaves once archives accumulate.
      - That updates and deletes do what the use cases expect.
  - **API/integration tests**
    - Use Quarkus’ testing support with a Postgres dev service (or local container) to drive the public endpoints:
      - Create warehouse → assert status code and response body.
      - Archive → assert that subsequent list/get calls reflect the change.
      - Replace → assert that the old warehouse is archived and the new one is visible with the same business unit code.

- **Store/legacy behaviour**
  - For the store side, I would unit test `StoreResource` using a mocked `LegacyStoreManagerGateway`:
    - Simulate successful and failing transactions.
    - Assert that the legacy gateway is only called in the success case (after commit), and never on rollback.

- **Keeping the suite healthy**
  - When a new rule is introduced, start by writing or adjusting a test that expresses the new behaviour, then adapt the implementation.
  - Run the full Maven test suite on every push/PR in CI so regressions are caught before merge.

---

## Git & GitHub Workflow (public repo)

To make the work easy to review I keep the git story simple and let CI do the heavy lifting:

1. **Initialize the repository locally (once)**
   - From the project root:
     ```bash
     git init
     git add .
     git commit -m "Initial import of warehouse assignment, CI and documentation"
     ```

2. **Create the public GitHub repository**
   - On GitHub, create a new **public** repository, for example `fcs-interview-code-assignment`.

3. **Wire up the remote and push**
   - Replace `<YOUR_GITHUB_USERNAME>` and `<REPO_NAME>` below:
     ```bash
     git remote add origin git@github.com:<YOUR_GITHUB_USERNAME>/<REPO_NAME>.git
     git branch -M main
     git push -u origin main
     ```

4. **Work in feature branches**
   - For any non‑trivial change I prefer a short‑lived feature branch:
     ```bash
     git checkout -b feature/warehouse-implementation
     # ...code & tests...
     git commit -am "Implement warehouse create/replace/archive use cases"
     git push -u origin feature/warehouse-implementation
     ```
   - Open a Pull Request, let GitHub Actions run the test suite, and only merge once CI is green.

---

## GitHub Actions CI Setup

I added a small GitHub Actions workflow so every push and pull request runs the Maven tests for the Java assignment.

- **Triggers**
  - Any `push`.
  - Any `pull_request`.
- **Environment**
  - `ubuntu-latest`.
  - JDK 17 via `actions/setup-java` with Maven caching enabled.
- **Main steps**
  1. Check out the repository.
  2. Set up Java 17.
  3. Run `./mvnw -B test` in the `java-assignment` module.
  4. Fail the build if any test fails so broken changes do not get merged by accident.

If needed, it is straightforward to extend this pipeline with static analysis, coverage, or image builds (see the “Additional Enhancements” section below).

---

## Additional Enhancements (Swagger, DX, and Quality)

- **OpenAPI / Swagger UI**
  - Quarkus already integrates with OpenAPI via SmallRye:
    - Ensure the OpenAPI extension is present so that specs are exposed at an endpoint like `/q/openapi`.
    - Enable Swagger UI (typically at `/q/swagger-ui`) so reviewers can discover and try warehouse and store endpoints via a browser.
  - Document in the main `README.md`:
    - The exact URLs for the OpenAPI JSON/YAML and Swagger UI.
    - How to start the app in dev mode (`./mvnw quarkus:dev`) and navigate to the docs.
  - Optionally enrich `StoreResource` (and any extra endpoints) with MicroProfile OpenAPI annotations such as:
    - `@Operation`, `@APIResponse`, `@Parameter`, and example payloads, so the generated Swagger UI is self‑documenting.

- **Client collections and API examples**
  - A Postman collection is provided at `docs/warehouse-store.postman_collection.json`:
    - It defines a `{{baseUrl}}` variable defaulting to `http://localhost:8080`.
    - It contains requests for listing, creating, retrieving, archiving, and replacing warehouses, as well as basic store listing and creation.
  - You can import this file directly into Postman to exercise the main flows without manually crafting requests.
  - Additionally, an “API examples” section in the `README.md` can showcase:
    - Sample JSON request/response bodies.
    - Example `curl` commands to create, retrieve, replace, and archive warehouses.

- **Code coverage**
  - Add JaCoCo to the `java-assignment` `pom.xml` to generate coverage reports as part of the Maven build.
  - Update the GitHub Actions workflow to run:
    - `./mvnw test jacoco:report`
  - (Optional) Integrate with a coverage service (e.g. Codecov) by:
    - Uploading the JaCoCo XML report in a dedicated CI step.
    - Enabling coverage badges and thresholds on PRs.

- **Static analysis and style**
  - Introduce tools like Checkstyle or SpotBugs into the Maven build:
    - Define a basic ruleset focused on obvious bugs and consistency rather than stylistic nit‑picking.
  - Update CI so that:
    - `./mvnw verify` runs static analysis, failing the pipeline on high‑severity findings.
  - Clearly document how to run these checks locally in the `README.md` to reduce friction for contributors.

- **Containerization integration**
  - Reuse existing Dockerfiles under `src/main/docker` to:
    - Build a Docker image for the monolith as part of CI, at least on the main branch.
  - Document in `README.md`:
    - Example commands to build and run the image locally.
    - How environment variables (e.g. DB connection) are configured for containers.

These additions are not strictly required for solving the assignment tasks, but they significantly improve developer experience, observability, and overall production‑readiness, which is valuable to demonstrate in an interview setting.

---

## Summary

- The assignment focuses on implementing **missing domain logic** and **correct transactional behavior** while respecting realistic warehouse and store constraints.
- The **warehouse module** is the richest in business rules and should be treated as the core domain area, with `Location` and `Store` acting as supporting contexts.
- A clear **testing strategy** and automated **CI with GitHub Actions** ensure that implementations remain correct, maintainable, and verifiable when the code is pushed to a public Git repository.

