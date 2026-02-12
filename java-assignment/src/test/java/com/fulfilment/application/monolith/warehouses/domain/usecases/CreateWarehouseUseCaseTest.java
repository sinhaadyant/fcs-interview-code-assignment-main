package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private InMemoryLocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    locationResolver = new InMemoryLocationResolver();
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void create_shouldPersistWarehouse_whenInputIsValid() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 10;
    warehouse.createdAt = LocalDateTime.now();

    assertDoesNotThrow(() -> useCase.create(warehouse));
    assertEquals(1, warehouseStore.getAll().size());
    assertEquals("MWH.100", warehouseStore.getAll().get(0).businessUnitCode);
  }

  @Test
  void create_shouldFail_whenBusinessUnitAlreadyExists() {
    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "MWH.100";
    existing.location = "AMSTERDAM-001";
    existing.capacity = 50;
    existing.stock = 10;
    existing.createdAt = LocalDateTime.now();
    warehouseStore.create(existing);

    Warehouse duplicate = new Warehouse();
    duplicate.businessUnitCode = "MWH.100";
    duplicate.location = "AMSTERDAM-001";
    duplicate.capacity = 50;
    duplicate.stock = 5;

    assertThrows(IllegalStateException.class, () -> useCase.create(duplicate));
  }

  @Test
  void create_shouldFail_whenLocationDoesNotExist() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.200";
    warehouse.location = "UNKNOWN-001";
    warehouse.capacity = 10;
    warehouse.stock = 5;

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  void create_shouldFail_whenMaxWarehousesForLocationIsReached() {
    // AMSTERDAM-001: maxNumberOfWarehouses = 2, fill it first
    Warehouse w1 = new Warehouse();
    w1.businessUnitCode = "MWH.201";
    w1.location = "AMSTERDAM-001";
    w1.capacity = 10;
    w1.stock = 5;
    warehouseStore.create(w1);

    Warehouse w2 = new Warehouse();
    w2.businessUnitCode = "MWH.202";
    w2.location = "AMSTERDAM-001";
    w2.capacity = 10;
    w2.stock = 5;
    warehouseStore.create(w2);

    Warehouse candidate = new Warehouse();
    candidate.businessUnitCode = "MWH.203";
    candidate.location = "AMSTERDAM-001";
    candidate.capacity = 10;
    candidate.stock = 5;

    assertThrows(IllegalStateException.class, () -> useCase.create(candidate));
  }

  @Test
  void create_shouldFail_whenTotalCapacityExceedsLocationMax() {
    // ZWOLLE-001: maxCapacity = 40
    Warehouse w1 = new Warehouse();
    w1.businessUnitCode = "MWH.300";
    w1.location = "ZWOLLE-001";
    w1.capacity = 30;
    w1.stock = 10;
    warehouseStore.create(w1);

    Warehouse candidate = new Warehouse();
    candidate.businessUnitCode = "MWH.301";
    candidate.location = "ZWOLLE-001";
    candidate.capacity = 20; // 30 + 20 > 40
    candidate.stock = 5;

    assertThrows(IllegalStateException.class, () -> useCase.create(candidate));
  }

  @Test
  void create_shouldFail_whenWarehouseIsNull() {
    assertThrows(IllegalArgumentException.class, () -> useCase.create(null));
  }

  @Test
  void create_shouldFail_whenBusinessUnitCodeIsBlank() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "   ";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 10;
    warehouse.stock = 5;
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  void create_shouldFail_whenCapacityIsNullOrZero() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.NC";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = null;
    warehouse.stock = 5;
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));

    warehouse.capacity = 0;
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  void create_shouldFail_whenStockIsNegative() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.NS";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 10;
    warehouse.stock = -1;
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  void create_shouldFail_whenStockExceedsCapacity() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.SC";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 10;
    warehouse.stock = 11;
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  void create_shouldFail_whenLocationIsBlank() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.BL";
    warehouse.location = "  ";
    warehouse.capacity = 10;
    warehouse.stock = 5;
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
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

  private static class InMemoryLocationResolver implements LocationResolver {

    private final List<Location> locations = new ArrayList<>();

    InMemoryLocationResolver() {
      locations.add(new Location("ZWOLLE-001", 1, 40));
      locations.add(new Location("AMSTERDAM-001", 2, 100));
    }

    @Override
    public Location resolveByIdentifier(String identifier) {
      return locations.stream()
          .filter(l -> l.identification.equals(identifier))
          .findFirst()
          .orElse(null);
    }
  }
}
