package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.domain.models.ProductFulfilmentAssignment;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ProductFulfilmentStore;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AssignWarehouseToProductForStoreUseCaseTest {

  private InMemoryProductFulfilmentStore productFulfilmentStore;
  private InMemoryWarehouseStore warehouseStore;
  private AssignWarehouseToProductForStoreUseCase useCase;

  @BeforeEach
  void setUp() {
    productFulfilmentStore = new InMemoryProductFulfilmentStore();
    warehouseStore = new InMemoryWarehouseStore();
    useCase = new AssignWarehouseToProductForStoreUseCase(productFulfilmentStore, warehouseStore);
  }

  @Test
  void assign_shouldCreateAssignment_whenWithinAllLimits() {
    Store store = new Store("STORE-1");
    store.id = 1L;
    Product product = new Product("PROD-1");
    product.id = 1L;
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.700";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 20;
    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);

    assertDoesNotThrow(() -> useCase.assign(store.id, warehouse.businessUnitCode, product.id));
    assertEquals(1, productFulfilmentStore.assignments.size());
  }

  @Test
  void assign_shouldFail_whenMoreThanTwoWarehousesPerProductAndStore() {
    Store store = new Store("STORE-1");
    store.id = 1L;
    Product product = new Product("PROD-1");
    product.id = 1L;

    warehouseStore.create(newWarehouse("MWH.701"));
    warehouseStore.create(newWarehouse("MWH.702"));
    warehouseStore.create(newWarehouse("MWH.703"));

    // two different warehouses already serve this product at this store
    productFulfilmentStore.assign(
        assignment(store.id, "MWH.701", product.id));
    productFulfilmentStore.assign(
        assignment(store.id, "MWH.702", product.id));

    assertThrows(
        IllegalStateException.class,
        () -> useCase.assign(store.id, "MWH.703", product.id));
  }

  @Test
  void assign_shouldFail_whenMoreThanThreeWarehousesForStore() {
    Store store = new Store("STORE-1");
    store.id = 1L;
    Product product = new Product("PROD-1");
    product.id = 1L;

    warehouseStore.create(newWarehouse("MWH.710"));
    warehouseStore.create(newWarehouse("MWH.711"));
    warehouseStore.create(newWarehouse("MWH.712"));
    warehouseStore.create(newWarehouse("MWH.713"));

    productFulfilmentStore.assign(assignment(store.id, "MWH.710", product.id));
    productFulfilmentStore.assign(assignment(store.id, "MWH.711", product.id));
    productFulfilmentStore.assign(assignment(store.id, "MWH.712", product.id));

    assertThrows(
        IllegalStateException.class,
        () -> useCase.assign(store.id, "MWH.713", product.id));
  }

  @Test
  void assign_shouldFail_whenMoreThanFiveProductsForWarehouse() {
    Store store = new Store("STORE-1");
    store.id = 1L;
    Warehouse warehouse = newWarehouse("MWH.720");
    warehouseStore.create(warehouse);

    // 5 products already assigned
    for (long i = 1; i <= 5; i++) {
      productFulfilmentStore.assign(assignment(store.id, "MWH.720", i));
    }

    assertThrows(
        IllegalStateException.class,
        () -> useCase.assign(store.id, "MWH.720", 6L));
  }

  private static ProductFulfilmentAssignment assignment(
      Long storeId, String warehouseBuCode, Long productId) {
    ProductFulfilmentAssignment a = new ProductFulfilmentAssignment();
    a.storeId = storeId;
    a.warehouseBusinessUnitCode = warehouseBuCode;
    a.productId = productId;
    return a;
  }

  private static Warehouse newWarehouse(String buCode) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = "AMSTERDAM-001";
    w.capacity = 100;
    w.stock = 10;
    w.createdAt = LocalDateTime.now();
    return w;
  }

  private static class InMemoryProductFulfilmentStore implements ProductFulfilmentStore {

    private final List<ProductFulfilmentAssignment> assignments = new ArrayList<>();

    @Override
    public void assign(ProductFulfilmentAssignment assignment) {
      assignments.add(assignment);
    }

    @Override
    public boolean exists(ProductFulfilmentAssignment assignment) {
      return assignments.stream()
          .anyMatch(
              a ->
                  a.storeId.equals(assignment.storeId)
                      && a.productId.equals(assignment.productId)
                      && a.warehouseBusinessUnitCode.equals(
                          assignment.warehouseBusinessUnitCode));
    }

    @Override
    public List<ProductFulfilmentAssignment> findByStoreAndProduct(
        Long storeId, Long productId) {
      List<ProductFulfilmentAssignment> result = new ArrayList<>();
      for (ProductFulfilmentAssignment a : assignments) {
        if (a.storeId.equals(storeId) && a.productId.equals(productId)) {
          result.add(a);
        }
      }
      return result;
    }

    @Override
    public List<ProductFulfilmentAssignment> findByStore(Long storeId) {
      List<ProductFulfilmentAssignment> result = new ArrayList<>();
      for (ProductFulfilmentAssignment a : assignments) {
        if (a.storeId.equals(storeId)) {
          result.add(a);
        }
      }
      return result;
    }

    @Override
    public List<ProductFulfilmentAssignment> findByWarehouseBusinessUnitCode(
        String businessUnitCode) {
      List<ProductFulfilmentAssignment> result = new ArrayList<>();
      for (ProductFulfilmentAssignment a : assignments) {
        if (a.warehouseBusinessUnitCode.equals(businessUnitCode)) {
          result.add(a);
        }
      }
      return result;
    }
  }

  private static class InMemoryWarehouseStore implements WarehouseStore {
    private final List<Warehouse> warehouses = new ArrayList<>();

    @Override
    public List<Warehouse> getAll() {
      return new ArrayList<>(warehouses);
    }

    @Override
    public void create(Warehouse warehouse) {
      warehouses.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {
      for (int i = 0; i < warehouses.size(); i++) {
        if (warehouses.get(i).businessUnitCode.equals(warehouse.businessUnitCode)) {
          warehouses.set(i, warehouse);
          return;
        }
      }
    }

    @Override
    public void remove(Warehouse warehouse) {
      warehouses.removeIf(w -> w.businessUnitCode.equals(warehouse.businessUnitCode));
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return warehouses.stream()
          .filter(w -> w.businessUnitCode.equals(buCode) && w.archivedAt == null)
          .findFirst()
          .orElse(null);
    }
  }
}

