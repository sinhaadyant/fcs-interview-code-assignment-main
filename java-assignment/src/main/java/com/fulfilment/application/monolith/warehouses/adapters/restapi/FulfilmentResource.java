package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.common.BusinessException;
import com.fulfilment.application.monolith.common.MessageService;
import com.fulfilment.application.monolith.warehouses.domain.ports.AssignWarehouseToProductForStoreOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * REST endpoint for product fulfilment: assign a warehouse to fulfil a product for a store.
 *
 * <p>Constraints: max 2 warehouses per product per store, max 3 warehouses per store,
 * max 5 products per warehouse. Returns 201 when created, 400 for bad request, 409 when
 * a constraint would be violated.
 */
@Path("store/{storeId}/fulfilment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentResource {

  private static final Logger LOGGER = Logger.getLogger(FulfilmentResource.class);

  @Inject AssignWarehouseToProductForStoreOperation assignOperation;
  @Inject MessageService messageService;

  @POST
  public Response assign(
      @PathParam("storeId") Long storeId,
      @Valid FulfilmentAssignRequest body) {
    if (storeId == null) {
      throw new BusinessException("error.bad_request", 400, "VAL_001");
    }
    if (body == null || body.warehouseBusinessUnitCode == null || body.productId == null) {
      throw new BusinessException("error.validation_failed", 400, "VAL_001");
    }

    try {
      assignOperation.assign(storeId, body.warehouseBusinessUnitCode, body.productId);
    } catch (IllegalArgumentException e) {
      String msg = e.getMessage() != null ? e.getMessage() : "";
      if (msg.contains("Warehouse not found")) {
        throw new BusinessException("fulfilment.warehouse_not_found", 400, "VAL_001");
      }
      throw new BusinessException("error.validation_failed", 400, "VAL_001");
    } catch (IllegalStateException e) {
      String msg = e.getMessage() != null ? e.getMessage() : "";
      String key = "generic.error";
      int status = 409;
      if (msg.contains("at most 2 warehouses per store")) {
        key = "fulfilment.max_warehouses_per_product";
      } else if (msg.contains("at most 3 different warehouses")) {
        key = "fulfilment.max_warehouses_per_store";
      } else if (msg.contains("at most 5 different product types")) {
        key = "fulfilment.max_products_per_warehouse";
      }
      throw new BusinessException(key, status, "BIZ_001");
    }

    LOGGER.infov("Fulfilment assigned storeId={0} warehouse={1} productId={2}",
        storeId, body.warehouseBusinessUnitCode, body.productId);
    return Response.status(201)
        .header("X-Message", messageService.get("fulfilment.assigned"))
        .build();
  }
}
