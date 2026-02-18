package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for assigning a warehouse to fulfil a product for a store.
 */
public class FulfilmentAssignRequest {

  @NotNull(message = "warehouseBusinessUnitCode is required")
  public String warehouseBusinessUnitCode;

  @NotNull(message = "productId is required")
  public Long productId;
}
