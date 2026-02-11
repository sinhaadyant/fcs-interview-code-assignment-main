package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.ProductFulfilmentAssignment;
import java.util.List;

public interface ProductFulfilmentStore {

  void assign(ProductFulfilmentAssignment assignment);

  boolean exists(ProductFulfilmentAssignment assignment);

  List<ProductFulfilmentAssignment> findByStoreAndProduct(Long storeId, Long productId);

  List<ProductFulfilmentAssignment> findByStore(Long storeId);

  List<ProductFulfilmentAssignment> findByWarehouseBusinessUnitCode(String businessUnitCode);
}

