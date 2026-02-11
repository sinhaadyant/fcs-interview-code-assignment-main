package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Use case responsible for replacing the current active warehouse for a given business unit code.
 *
 * <p>The operation archives the existing warehouse and creates a new one under the same business
 * unit code while enforcing:
 *
 * <ul>
 *   <li>Stock of the new warehouse must exactly match the previous one.
 *   <li>New capacity must be able to hold the existing stock.
 *   <li>Location must exist and respect per-location warehouse and capacity limits.
 * </ul>
 */
@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse == null) {
      throw new IllegalArgumentException("New warehouse must not be null");
    }

    if (newWarehouse.businessUnitCode == null || newWarehouse.businessUnitCode.isBlank()) {
      throw new IllegalArgumentException("Business unit code must be provided");
    }

    Warehouse current =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (current == null) {
      throw new IllegalStateException(
          "No active warehouse found to replace for business unit code: "
              + newWarehouse.businessUnitCode);
    }

    if (newWarehouse.capacity == null || newWarehouse.capacity <= 0) {
      throw new IllegalArgumentException("Capacity must be a positive number");
    }

    if (newWarehouse.stock == null || newWarehouse.stock < 0) {
      throw new IllegalArgumentException("Stock must be zero or a positive number");
    }

    if (!newWarehouse.stock.equals(current.stock)) {
      throw new IllegalStateException(
          "New warehouse stock must match the stock of the warehouse being replaced");
    }

    if (newWarehouse.capacity < current.stock) {
      throw new IllegalStateException(
          "New warehouse capacity must be able to accommodate existing stock");
    }

    if (newWarehouse.location == null || newWarehouse.location.isBlank()) {
      throw new IllegalArgumentException("Location must be provided");
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new IllegalArgumentException(
          "Location not found for identifier: " + newWarehouse.location);
    }

    // Validate number of warehouses and capacity constraints for the new location,
    // considering that the current warehouse will be archived.
    var activeInLocationExcludingCurrent =
        warehouseStore.getAll().stream()
            .filter(
                w ->
                    w.archivedAt == null
                        && !w.businessUnitCode.equals(current.businessUnitCode)
                        && location.identification.equals(w.location))
            .toList();

    if (activeInLocationExcludingCurrent.size() >= location.maxNumberOfWarehouses) {
      throw new IllegalStateException(
          "Maximum number of warehouses reached for location: " + location.identification);
    }

    int capacitySumExcludingCurrent =
        activeInLocationExcludingCurrent.stream()
            .mapToInt(w -> w.capacity != null ? w.capacity : 0)
            .sum();
    if (capacitySumExcludingCurrent + newWarehouse.capacity > location.maxCapacity) {
      throw new IllegalStateException(
          "Total capacity for location "
              + location.identification
              + " would exceed its maximum allowed capacity");
    }

    // Archive current warehouse.
    current.archivedAt = java.time.LocalDateTime.now();
    warehouseStore.update(current);

    // Create the replacement warehouse as a new active record for this business unit code.
    newWarehouse.createdAt = java.time.LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}
