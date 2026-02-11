package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Use case that archives an active warehouse for a given business unit code.
 *
 * <p>We do not hard-delete warehouses – instead we mark them as archived so that history is
 * preserved. Only one active warehouse per business unit code is allowed at any time.
 */
@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    if (warehouse == null) {
      throw new IllegalArgumentException("Warehouse must not be null");
    }

    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new IllegalArgumentException("Business unit code must be provided");
    }

    Warehouse current =
        warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (current == null) {
      throw new IllegalStateException(
          "No active warehouse found to archive for business unit code: "
              + warehouse.businessUnitCode);
    }

    current.archivedAt = java.time.LocalDateTime.now();

    warehouseStore.update(current);
  }
}
