package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.ProductFulfilmentAssignment;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "product_fulfilment",
    uniqueConstraints = {
      @UniqueConstraint(
          columnNames = {"storeId", "warehouseBusinessUnitCode", "productId"})
    })
@Cacheable
public class DbProductFulfilment {

  @Id @GeneratedValue public Long id;

  @Column(nullable = false)
  public Long storeId;

  @Column(nullable = false, length = 40)
  public String warehouseBusinessUnitCode;

  @Column(nullable = false)
  public Long productId;

  public DbProductFulfilment() {}

  public static DbProductFulfilment fromAssignment(ProductFulfilmentAssignment assignment) {
    DbProductFulfilment entity = new DbProductFulfilment();
    entity.storeId = assignment.storeId;
    entity.warehouseBusinessUnitCode = assignment.warehouseBusinessUnitCode;
    entity.productId = assignment.productId;
    return entity;
  }

  public ProductFulfilmentAssignment toAssignment() {
    ProductFulfilmentAssignment assignment = new ProductFulfilmentAssignment();
    assignment.storeId = this.storeId;
    assignment.warehouseBusinessUnitCode = this.warehouseBusinessUnitCode;
    assignment.productId = this.productId;
    return assignment;
  }
}

