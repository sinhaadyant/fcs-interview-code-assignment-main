package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Use case responsible for creating a new warehouse unit.
 *
 * <p>This encapsulates all business rules around:
 *
 * <ul>
 *   <li>Business unit code uniqueness
 *   <li>Location existence
 *   <li>Per-location maximum number of active warehouses
 *   <li>Total capacity constraints per location
 *   <li>Basic capacity/stock sanity checks
 * </ul>
 */
@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouse == null) {
      throw new IllegalArgumentException("Warehouse must not be null");
    }

    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new IllegalArgumentException("Business unit code must be provided");
    }

    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new IllegalArgumentException("Capacity must be a positive number");
    }

    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new IllegalArgumentException("Stock must be zero or a positive number");
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new IllegalArgumentException("Stock cannot be greater than capacity");
    }

    // Business unit must be unique among active warehouses.
    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null) {
      throw new IllegalStateException(
          "Active warehouse with business unit code already exists: "
              + warehouse.businessUnitCode);
    }

    // Location must exist and be recognised by the domain catalog.
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new IllegalArgumentException("Location must be provided");
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new IllegalArgumentException(
          "Location not found for identifier: " + warehouse.location);
    }

    // Validate number of warehouses and capacity constraints for this location.
    var activeInLocation =
        warehouseStore.getAll().stream()
            .filter(
                w ->
                    w.archivedAt == null
                        && location.identification.equals(w.location))
            .toList();

    if (activeInLocation.size() >= location.maxNumberOfWarehouses) {
      throw new IllegalStateException(
          "Maximum number of warehouses reached for location: " + location.identification);
    }

    int currentCapacitySum =
        activeInLocation.stream().mapToInt(w -> w.capacity != null ? w.capacity : 0).sum();
    if (currentCapacitySum + warehouse.capacity > location.maxCapacity) {
      throw new IllegalStateException(
          "Total capacity for location "
              + location.identification
              + " would exceed its maximum allowed capacity");
    }

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }
}
