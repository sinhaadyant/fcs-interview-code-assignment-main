package com.fulfilment.application.monolith.warehouses.domain.models;

/**
 * Domain representation of a fulfilment assignment: a given product can be
 * fulfilled for a given store by a specific warehouse.
 */
public class ProductFulfilmentAssignment {

  public Long storeId;

  public String warehouseBusinessUnitCode;

  public Long productId;
}

