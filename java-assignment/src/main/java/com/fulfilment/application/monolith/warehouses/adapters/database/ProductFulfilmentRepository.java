package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.ProductFulfilmentAssignment;
import com.fulfilment.application.monolith.warehouses.domain.ports.ProductFulfilmentStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Panache-based implementation of {@link ProductFulfilmentStore}.
 *
 * <p>This is a thin adapter that translates between the JPA entity {@link DbProductFulfilment}
 * and the domain model {@link ProductFulfilmentAssignment}. All business rules around assignments
 * live in the corresponding use case.
 */
@ApplicationScoped
public class ProductFulfilmentRepository
    implements ProductFulfilmentStore, PanacheRepository<DbProductFulfilment> {

  @Override
  public void assign(ProductFulfilmentAssignment assignment) {
    persist(DbProductFulfilment.fromAssignment(assignment));
  }

  @Override
  public boolean exists(ProductFulfilmentAssignment assignment) {
    return count(
            "storeId = ?1 and warehouseBusinessUnitCode = ?2 and productId = ?3",
            assignment.storeId,
            assignment.warehouseBusinessUnitCode,
            assignment.productId)
        > 0;
  }

  @Override
  public List<ProductFulfilmentAssignment> findByStoreAndProduct(Long storeId, Long productId) {
    return find("storeId = ?1 and productId = ?2", storeId, productId).stream()
        .map(DbProductFulfilment::toAssignment)
        .toList();
  }

  @Override
  public List<ProductFulfilmentAssignment> findByStore(Long storeId) {
    return find("storeId = ?1", storeId).stream()
        .map(DbProductFulfilment::toAssignment)
        .toList();
  }

  @Override
  public List<ProductFulfilmentAssignment> findByWarehouseBusinessUnitCode(
      String businessUnitCode) {
    return find("warehouseBusinessUnitCode = ?1", businessUnitCode).stream()
        .map(DbProductFulfilment::toAssignment)
        .toList();
  }
}

