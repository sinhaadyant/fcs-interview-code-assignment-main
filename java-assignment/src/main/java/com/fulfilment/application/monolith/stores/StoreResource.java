package com.fulfilment.application.monolith.stores;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.logging.Logger;

/**
 * REST endpoint for managing {@link Store} entities.
 *
 * <p>Besides the usual CRUD operations this resource is responsible for coordinating updates with
 * the {@link LegacyStoreManagerGateway}. The key requirement is that the legacy system is only
 * called once the local database transaction has successfully committed, which we enforce using
 * {@link jakarta.transaction.TransactionSynchronizationRegistry}.
 */
@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Inject TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  private static final Logger LOGGER = Logger.getLogger(StoreResource.class.getName());

  @GET
  public List<Store> get(
      @QueryParam("page") Integer page,
      @QueryParam("size") Integer size) {
    // Defensive defaults so that callers are not forced to pass pagination params.
    int pageIndex = page != null && page >= 0 ? page : 0;
    int pageSize = size != null && size > 0 ? size : 50;
    LOGGER.infov("Listing stores page={0}, size={1}", pageIndex, pageSize);
    return Store.findAll(Sort.by("name"))
        .page(Page.of(pageIndex, pageSize))
        .list();
  }

  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  @Transactional
  public Response create(@jakarta.validation.Valid Store store) {
    if (store.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    LOGGER.infov("Creating store name={0}", store.name);
    store.persist();

    // Register a callback that will only execute after the JTA transaction completes.
    transactionSynchronizationRegistry.registerInterposedSynchronization(
        new Synchronization() {
          @Override
          public void beforeCompletion() {}

          @Override
          public void afterCompletion(int status) {
            if (status == jakarta.transaction.Status.STATUS_COMMITTED) {
              legacyStoreManagerGateway.createStoreOnLegacySystem(store);
            }
          }
        });

    return Response.ok(store).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Store update(Long id, @jakarta.validation.Valid Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    entity.name = updatedStore.name;
    entity.quantityProductsInStock = updatedStore.quantityProductsInStock;

    LOGGER.infov("Updating store id={0}, name={1}", id, updatedStore.name);

    // Same pattern as in create(): only sync to legacy once the transaction has committed.
    transactionSynchronizationRegistry.registerInterposedSynchronization(
        new Synchronization() {
          @Override
          public void beforeCompletion() {}

          @Override
          public void afterCompletion(int status) {
            if (status == jakarta.transaction.Status.STATUS_COMMITTED) {
              legacyStoreManagerGateway.updateStoreOnLegacySystem(entity);
            }
          }
        });

    return entity;
  }

  @PATCH
  @Path("{id}")
  @Transactional
  public Store patch(Long id, @jakarta.validation.Valid Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    if (updatedStore.name != null) {
      entity.name = updatedStore.name;
    }

    if (updatedStore.quantityProductsInStock != 0) {
      entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
    }

    LOGGER.infov("Patching store id={0}, name={1}", id, entity.name);

    transactionSynchronizationRegistry.registerInterposedSynchronization(
        new Synchronization() {
          @Override
          public void beforeCompletion() {}

          @Override
          public void afterCompletion(int status) {
            if (status == jakarta.transaction.Status.STATUS_COMMITTED) {
              legacyStoreManagerGateway.updateStoreOnLegacySystem(entity);
            }
          }
        });

    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    LOGGER.infov("Deleting store id={0}", id);
    entity.delete();
    return Response.status(204).build();
  }

  @Provider
  public static class ErrorMapper implements ExceptionMapper<Exception> {

    @Inject ObjectMapper objectMapper;

    @Override
    public Response toResponse(Exception exception) {
      LOGGER.error("Failed to handle request", exception);

      int code = 500;
      if (exception instanceof WebApplicationException) {
        code = ((WebApplicationException) exception).getResponse().getStatus();
      }

      ObjectNode exceptionJson = objectMapper.createObjectNode();
      exceptionJson.put("exceptionType", exception.getClass().getName());
      exceptionJson.put("code", code);

      if (exception.getMessage() != null) {
        exceptionJson.put("error", exception.getMessage());
      }

      return Response.status(code).entity(exceptionJson).build();
    }
  }
}
