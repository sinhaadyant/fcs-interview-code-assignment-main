package com.fulfilment.application.monolith.stores;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "store")
@Cacheable
public class Store extends PanacheEntity {

  @Column(length = 40, unique = true)
  @NotBlank(message = "Store name must not be blank")
  public String name;

  @Min(value = 0, message = "Quantity in stock must be zero or positive")
  public int quantityProductsInStock;

  public Store() {}

  public Store(String name) {
    this.name = name;
  }
}
