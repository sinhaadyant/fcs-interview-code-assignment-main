package com.fulfilment.application.monolith.warehouses.domain.ports;

/**
 * Use case for assigning a warehouse to fulfil a given product for a given store,
 * enforcing global constraints on the number of assignments.
 */
public interface AssignWarehouseToProductForStoreOperation {

  void assign(Long storeId, String warehouseBusinessUnitCode, Long productId);
}

