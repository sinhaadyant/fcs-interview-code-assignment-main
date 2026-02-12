package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Inject
  EntityManager entityManager;

  private static final String ACTIVE_FILTER = "archivedAt is null";

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  /** Returns only active (non-archived) warehouses for list endpoint consistency with get-by-id. */
  public List<DbWarehouse> findActivePage(Page page) {
    return find(ACTIVE_FILTER).page(page).list();
  }

  /** For REST layer: return persisted entity (e.g. after create) to include id in response. */
  public DbWarehouse findEntityByBusinessUnitCode(String buCode) {
    return find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse entity = DbWarehouse.fromWarehouse(warehouse);
    this.persist(entity);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse entity =
        find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
            .firstResult();
    if (entity == null) {
      return;
    }
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
    // Entity is managed; Hibernate dirty-checking persists changes at flush/commit.
    entityManager.flush();
  }

  @Override
  public void remove(Warehouse warehouse) {
    delete("businessUnitCode = ?1", warehouse.businessUnitCode);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse entity =
        find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
    return entity != null ? entity.toWarehouse() : null;
  }
}
