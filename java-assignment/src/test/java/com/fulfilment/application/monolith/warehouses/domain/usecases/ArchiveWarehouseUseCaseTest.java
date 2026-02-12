package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  void archive_shouldSetArchivedAt_whenWarehouseExistsAndIsActive() {
    Warehouse current = new Warehouse();
    current.businessUnitCode = "MWH.600";
    current.location = "AMSTERDAM-001";
    current.capacity = 50;
    current.stock = 10;
    current.createdAt = LocalDateTime.now().minusDays(1);
    warehouseStore.create(current);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "MWH.600";

    useCase.archive(request);

    Warehouse stored = warehouseStore.findByBusinessUnitCode("MWH.600");
    // findByBusinessUnitCode only returns active warehouses, so archived one should be null
    assertEquals(null, stored);

    Warehouse archived = warehouseStore.getArchivedByBusinessUnitCode("MWH.600");
    assertNotNull(archived.archivedAt);
  }

  @Test
  void archive_shouldFail_whenNoActiveWarehouseExists() {
    Warehouse request = new Warehouse();
    request.businessUnitCode = "UNKNOWN";

    assertThrows(IllegalStateException.class, () -> useCase.archive(request));
  }

  @Test
  void archive_shouldFail_whenWarehouseIsNull() {
    assertThrows(IllegalArgumentException.class, () -> useCase.archive(null));
  }

  @Test
  void archive_shouldFail_whenBusinessUnitCodeIsBlank() {
    Warehouse request = new Warehouse();
    request.businessUnitCode = "   ";
    assertThrows(IllegalArgumentException.class, () -> useCase.archive(request));
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

    Warehouse getArchivedByBusinessUnitCode(String buCode) {
      return warehouses.stream()
          .filter(w -> w.businessUnitCode.equals(buCode) && w.archivedAt != null)
          .findFirst()
          .orElse(null);
    }
  }
}
