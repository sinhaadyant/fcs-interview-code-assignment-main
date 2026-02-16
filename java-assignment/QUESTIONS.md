# Design and improvement questions

Perspective: senior principal engineer. As per [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md).

---

## Q1. Unify persistence styles (Store/Product vs Warehouse)?

**Question:** Should we unify the persistence approach—Store/Product use Panache entities and repositories directly in the resource, while Warehouse uses ports + adapters with a separate domain model and JPA entity? How would you approach it?

**Answer:** Recommend keeping the current split and documenting it explicitly. Warehouse justifies ports/adapters: non-trivial domain (replace, archive, location and capacity rules) and a clear boundary between API contract and persistence. Store and Product are simple CRUD; adding a port layer there would add indirection without clear benefit unless we anticipate similar domain growth. If we ever unify: introduce a port (e.g. `StoreStore`) and a thin adapter over the existing Panache repository so the resource depends only on the port. Principle: match the abstraction to the complexity.

---

## Q2. OpenAPI for Store and Product too?

**Question:** Warehouse is contract-first (OpenAPI spec, generated interface). Should we add OpenAPI specs for Store and Product and generate their resources as well?

**Answer:** Yes if the organisation cares about a single source of truth for all public APIs, client generation, and contract testing. Then we treat all three resources the same: spec first, generate interface, implement. If the priority is speed and the team is small, hand-written Store/Product plus SmallRye OpenAPI scan is acceptable; we trade some consistency for less upfront spec work. Decide based on whether multiple consumers or teams will depend on these APIs.

---

## Q3. Expose an HTTP endpoint for product-fulfilment assign?

**Question:** The bonus product-fulfilment use case (`AssignWarehouseToProductForStoreOperation`) is implemented and tested at the use-case level. Should we add a REST endpoint that calls it?

**Answer:** Yes. The capability exists; exposing it completes the API surface and keeps behaviour testable via HTTP as well as unit tests. Add one endpoint (e.g. `POST` with storeId, warehouseBusinessUnitCode, productId), delegate to the use case, return 201/400/409 as appropriate. Document the business constraints (max 2 warehouses per product per store, etc.) in the OpenAPI description and cover them in contract or integration tests. No new domain logic—adapter only.
