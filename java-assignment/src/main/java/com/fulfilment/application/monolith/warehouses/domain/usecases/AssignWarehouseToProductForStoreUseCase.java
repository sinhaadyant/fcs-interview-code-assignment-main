package com.fulfilment.application.monolith.warehouses.domain.usecases;
import com.fulfilment.application.monolith.warehouses.domain.models.ProductFulfilmentAssignment;
import com.fulfilment.application.monolith.warehouses.domain.ports.AssignWarehouseToProductForStoreOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ProductFulfilmentStore;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Use case that assigns a warehouse to fulfil a given product for a given store.
 *
 * <p>It enforces the three global constraints defined in the assignment:
 *
 * <ul>
 *   <li>A product can be fulfilled by at most 2 warehouses per store.
 *   <li>A store can be fulfilled by at most 3 different warehouses.
 *   <li>A warehouse can store at most 5 different product types.
 * </ul>
 */
@ApplicationScoped
public class AssignWarehouseToProductForStoreUseCase
    implements AssignWarehouseToProductForStoreOperation {

  private final ProductFulfilmentStore productFulfilmentStore;
  private final WarehouseStore warehouseStore;

  public AssignWarehouseToProductForStoreUseCase(
      ProductFulfilmentStore productFulfilmentStore, WarehouseStore warehouseStore) {
    this.productFulfilmentStore = productFulfilmentStore;
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void assign(Long storeId, String warehouseBusinessUnitCode, Long productId) {
    if (storeId == null || warehouseBusinessUnitCode == null || productId == null) {
      throw new IllegalArgumentException("Store, warehouse and product identifiers are required");
    }

    if (warehouseStore.findByBusinessUnitCode(warehouseBusinessUnitCode) == null) {
      throw new IllegalArgumentException(
          "Warehouse not found for business unit code: " + warehouseBusinessUnitCode);
    }

    ProductFulfilmentAssignment assignment = new ProductFulfilmentAssignment();
    assignment.storeId = storeId;
    assignment.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
    assignment.productId = productId;

    if (productFulfilmentStore.exists(assignment)) {
      // Idempotent behaviour: silently return if the exact assignment already exists.
      return;
    }

    // 1. Each Product can be fulfilled by a maximum of 2 different Warehouses per Store
    long warehousesForProductAtStore =
        productFulfilmentStore.findByStoreAndProduct(storeId, productId).stream()
            .map(a -> a.warehouseBusinessUnitCode)
            .distinct()
            .count();

    if (warehousesForProductAtStore >= 2) {
      throw new IllegalStateException(
          "A product can be fulfilled by at most 2 warehouses per store");
    }

    // 2. Each Store can be fulfilled by a maximum of 3 different Warehouses
    long warehousesForStore =
        productFulfilmentStore.findByStore(storeId).stream()
            .map(a -> a.warehouseBusinessUnitCode)
            .distinct()
            .count();

    if (warehousesForStore >= 3
        && productFulfilmentStore.findByStore(storeId).stream()
            .map(a -> a.warehouseBusinessUnitCode)
            .noneMatch(code -> code.equals(warehouseBusinessUnitCode))) {
      throw new IllegalStateException(
          "A store can be fulfilled by at most 3 different warehouses");
    }

    // 3. Each Warehouse can store maximally 5 types of Products
    long productsForWarehouse =
        productFulfilmentStore.findByWarehouseBusinessUnitCode(warehouseBusinessUnitCode).stream()
            .map(a -> a.productId)
            .distinct()
            .count();

    if (productsForWarehouse >= 5
        && productFulfilmentStore.findByWarehouseBusinessUnitCode(warehouseBusinessUnitCode).stream()
            .map(a -> a.productId)
            .noneMatch(id -> id.equals(productId))) {
      throw new IllegalStateException(
          "A warehouse can store at most 5 different product types");
    }

    productFulfilmentStore.assign(assignment);
  }
}

