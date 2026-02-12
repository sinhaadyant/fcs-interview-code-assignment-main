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

public class ReplaceWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private InMemoryLocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    locationResolver = new InMemoryLocationResolver();
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void replace_shouldArchiveCurrentAndCreateNew_whenInputIsValid() {
    Warehouse current = new Warehouse();
    current.businessUnitCode = "MWH.500";
    current.location = "AMSTERDAM-001";
    current.capacity = 50;
    current.stock = 10;
    current.createdAt = LocalDateTime.now().minusDays(10);
    warehouseStore.create(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.500";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 60;
    replacement.stock = 10; // same stock

    assertDoesNotThrow(() -> useCase.replace(replacement));

    // After replacement, we should still have one active warehouse for this BU code
    Warehouse active = warehouseStore.findByBusinessUnitCode("MWH.500");
    assertEquals("AMSTERDAM-001", active.location);
    assertEquals(60, active.capacity);
    assertEquals(10, active.stock);
  }

  @Test
  void replace_shouldFail_whenNoCurrentWarehouseExists() {
    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "UNKNOWN";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 10;
    replacement.stock = 5;

    assertThrows(IllegalStateException.class, () -> useCase.replace(replacement));
  }

  @Test
  void replace_shouldFail_whenStockDoesNotMatch() {
    Warehouse current = new Warehouse();
    current.businessUnitCode = "MWH.501";
    current.location = "AMSTERDAM-001";
    current.capacity = 50;
    current.stock = 10;
    current.createdAt = LocalDateTime.now().minusDays(5);
    warehouseStore.create(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.501";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 60;
    replacement.stock = 9; // different stock

    assertThrows(IllegalStateException.class, () -> useCase.replace(replacement));
  }

  @Test
  void replace_shouldFail_whenCapacityCannotHoldExistingStock() {
    Warehouse current = new Warehouse();
    current.businessUnitCode = "MWH.502";
    current.location = "AMSTERDAM-001";
    current.capacity = 50;
    current.stock = 10;
    current.createdAt = LocalDateTime.now().minusDays(5);
    warehouseStore.create(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.502";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 5; // less than existing stock
    replacement.stock = 10;

    assertThrows(IllegalStateException.class, () -> useCase.replace(replacement));
  }

  @Test
  void replace_shouldFail_whenCapacityIsNullOrInvalid() {
    Warehouse current = new Warehouse();
    current.businessUnitCode = "MWH.CAP";
    current.location = "AMSTERDAM-001";
    current.capacity = 50;
    current.stock = 10;
    current.createdAt = LocalDateTime.now();
    warehouseStore.create(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.CAP";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = null;
    replacement.stock = 10;
    assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));

    replacement.capacity = 0;
    assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
  }

  @Test
  void replace_shouldFail_whenStockIsNegative() {
    Warehouse current = new Warehouse();
    current.businessUnitCode = "MWH.NEG";
    current.location = "AMSTERDAM-001";
    current.capacity = 50;
    current.stock = 10;
    current.createdAt = LocalDateTime.now();
    warehouseStore.create(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.NEG";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 60;
    replacement.stock = -1;
    assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
  }

  @Test
  void replace_shouldFail_whenNewWarehouseIsNull() {
    assertThrows(IllegalArgumentException.class, () -> useCase.replace(null));
  }

  @Test
  void replace_shouldFail_whenBusinessUnitCodeIsBlank() {
    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "  ";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 10;
    replacement.stock = 5;
    assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
  }

  @Test
  void replace_shouldFail_whenLocationNotFound() {
    Warehouse current = new Warehouse();
    current.businessUnitCode = "MWH.LOC";
    current.location = "AMSTERDAM-001";
    current.capacity = 50;
    current.stock = 10;
    current.createdAt = LocalDateTime.now();
    warehouseStore.create(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.LOC";
    replacement.location = "UNKNOWN-LOC";
    replacement.capacity = 60;
    replacement.stock = 10;
    assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
  }

  @Test
  void replace_shouldFail_whenLocationMaxWarehousesReached() {
    // Use a resolver with max 2 warehouses at AMSTERDAM-001 so A1+A3 (excluding A2) = 2 >= 2
    locationResolver = new InMemoryLocationResolver(2, 200);
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
    warehouseStore.create(newWarehouse("MWH.A1", "AMSTERDAM-001"));
    warehouseStore.create(newWarehouse("MWH.A2", "AMSTERDAM-001"));
    warehouseStore.create(newWarehouse("MWH.A3", "AMSTERDAM-001"));

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.A2";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 60;
    replacement.stock = 5;
    assertThrows(IllegalStateException.class, () -> useCase.replace(replacement));
  }

  private static Warehouse newWarehouse(String buCode, String location) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = location;
    w.capacity = 50;
    w.stock = 10;
    w.createdAt = LocalDateTime.now();
    return w;
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
      locations.add(new Location("AMSTERDAM-001", 3, 200));
    }

    InMemoryLocationResolver(int maxWarehouses, int maxCapacity) {
      locations.add(new Location("AMSTERDAM-001", maxWarehouses, maxCapacity));
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
