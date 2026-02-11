package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * REST adapter for the generated {@link WarehouseResource} interface.
 *
 * <p>This class is intentionally thin – it maps HTTP requests to domain models and delegates to
 * the appropriate use cases. All business rules live in the domain; this layer is responsible for:
 *
 * <ul>
 *   <li>Pagination of list endpoints.
 *   <li>Mapping between generated API beans and domain models.
 *   <li>Logging of high-level operations for observability.
 * </ul>
 */
@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  private static final Logger LOGGER = Logger.getLogger(WarehouseResourceImpl.class);

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Context UriInfo uriInfo;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    int pageIndex = parseIntQueryParam("page", 0);
    int pageSize = parseIntQueryParam("size", 50);
    LOGGER.infov("Listing warehouses page={0}, size={1}", pageIndex, pageSize);
    return warehouseRepository.findAll()
        .page(Page.of(pageIndex, pageSize))
        .stream()
        .map(DbWarehouse::toWarehouse)
        .map(this::toWarehouseResponse)
        .toList();
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        toDomainWarehouse(data);
    LOGGER.infov(
        "Creating warehouse businessUnitCode={0}, location={1}",
        warehouse.businessUnitCode, warehouse.location);
    createWarehouseOperation.create(warehouse);
    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    if (id == null) {
      return null;
    }

    Long dbId;
    try {
      dbId = Long.valueOf(id);
    } catch (NumberFormatException ex) {
      return null;
    }

    DbWarehouse dbWarehouse = warehouseRepository.findById(dbId);
    if (dbWarehouse == null || dbWarehouse.archivedAt != null) {
      return null;
    }

    return toWarehouseResponse(dbWarehouse.toWarehouse());
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    if (id == null) {
      return;
    }

    Long dbId;
    try {
      dbId = Long.valueOf(id);
    } catch (NumberFormatException ex) {
      return;
    }

    DbWarehouse dbWarehouse = warehouseRepository.findById(dbId);
    if (dbWarehouse == null || dbWarehouse.archivedAt != null) {
      return;
    }

    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        dbWarehouse.toWarehouse();
    LOGGER.infov(
        "Archiving warehouse id={0}, businessUnitCode={1}",
        dbId, warehouse.businessUnitCode);
    archiveWarehouseOperation.archive(warehouse);
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        toDomainWarehouse(data);
    warehouse.businessUnitCode = businessUnitCode;
    LOGGER.infov("Replacing warehouse businessUnitCode={0}", businessUnitCode);
    replaceWarehouseOperation.replace(warehouse);
    return toWarehouseResponse(warehouse);
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainWarehouse(
      Warehouse data) {
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }

  private int parseIntQueryParam(String name, int defaultValue) {
    if (uriInfo == null || uriInfo.getQueryParameters() == null) {
      return defaultValue;
    }
    String value = uriInfo.getQueryParameters().getFirst(name);
    if (value == null) {
      return defaultValue;
    }
    try {
      int parsed = Integer.parseInt(value);
      return parsed >= 0 ? parsed : defaultValue;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
