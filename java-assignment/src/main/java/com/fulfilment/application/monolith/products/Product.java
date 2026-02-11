package com.fulfilment.application.monolith.products;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Entity
@Cacheable
public class Product {

  @Id @GeneratedValue public Long id;

  @Column(length = 40, unique = true)
  @NotBlank(message = "Product name must not be blank")
  public String name;

  @Column(nullable = true)
  public String description;

  @Column(precision = 10, scale = 2, nullable = true)
  @PositiveOrZero(message = "Price must be zero or positive")
  public BigDecimal price;

  @Min(value = 0, message = "Stock must be zero or positive")
  public int stock;

  public Product() {}

  public Product(String name) {
    this.name = name;
  }
}
