package com.fulfilment.application.monolith.products;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import com.fulfilment.application.monolith.common.BusinessException;
import com.fulfilment.application.monolith.common.MessageService;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * REST endpoint for managing {@link Product} entities.
 *
 * <p>For this assignment we expose a simple CRUD API, with pagination support for listing and
 * basic Bean Validation on the entity itself. In a larger system we would likely introduce
 * dedicated DTOs and a service layer in between.
 */
@Path("product")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class ProductResource {

  @Inject ProductRepository productRepository;
  @Inject MessageService messageService;

  private static final Logger LOGGER = Logger.getLogger(ProductResource.class.getName());

  @GET
  public List<Product> get(
      @QueryParam("page") Integer page,
      @QueryParam("size") Integer size) {
    // Default page/size keep the endpoint backwards compatible for callers that do not paginate.
    int pageIndex = page != null && page >= 0 ? page : 0;
    int pageSize = size != null && size > 0 ? size : 50;
    LOGGER.infov("Listing products page={0}, size={1}", pageIndex, pageSize);
    return productRepository.findAll(Sort.by("name"))
        .page(Page.of(pageIndex, pageSize))
        .list();
  }

  @GET
  @Path("{id}")
  public Product getSingle(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new BusinessException("product.not_found", 404, "NF_001", id);
    }
    return entity;
  }

  @POST
  @Transactional
  public Response create(@jakarta.validation.Valid Product product) {
    if (product.id != null) {
      throw new BusinessException("product.id_invalid", 422, "VAL_001");
    }

    LOGGER.infov("Creating product name={0}", product.name);
    productRepository.persist(product);
    return Response.ok(product).status(201).header("X-Message", messageService.get("product.created")).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Product update(Long id, @jakarta.validation.Valid Product product) {
    if (product.name == null) {
      throw new BusinessException("product.name_required", 422, "VAL_001");
    }

    Product entity = productRepository.findById(id);

    if (entity == null) {
      throw new BusinessException("product.not_found", 404, "NF_001", id);
    }

    entity.name = product.name;
    entity.description = product.description;
    entity.price = product.price;
    entity.stock = product.stock;

    LOGGER.infov("Updating product id={0}, name={1}", id, product.name);
    productRepository.persist(entity);

    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new BusinessException("product.not_found", 404, "NF_001", id);
    }
    LOGGER.infov("Deleting product id={0}", id);
    productRepository.delete(entity);
    return Response.status(204).build();
  }
}
