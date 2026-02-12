package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreEndpointTest {

  @Test
  public void testCrudStore() {
    final String path = "store";

    // List all, should have 3 stores initially from import.sql
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Create a new store
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "NEW_STORE",
              "quantityProductsInStock": 5
            }
            """)
        .when()
        .post(path)
        .then()
        .statusCode(201)
        .body(containsString("NEW_STORE"));

    // Update the newly created store (id 4 after import.sql)
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "NEW_STORE_UPDATED",
              "quantityProductsInStock": 10
            }
            """)
        .when()
        .put(path + "/4")
        .then()
        .statusCode(200)
        .body(containsString("NEW_STORE_UPDATED"));

    // Patch the updated store
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "NEW_STORE_PATCHED",
              "quantityProductsInStock": 15
            }
            """)
        .when()
        .patch(path + "/4")
        .then()
        .statusCode(200)
        .body(containsString("NEW_STORE_PATCHED"));

    // Delete the KALLAX store (id 2)
    given().when().delete(path + "/2").then().statusCode(204);

    // List all, KALLAX should be missing now and patched store present
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString("KALLAX")),
            containsString("TONSTAD"),
            containsString("BESTÅ"),
            containsString("NEW_STORE_PATCHED"));
  }

  @Test
  public void getSingle_shouldReturn404WhenStoreNotFound() {
    given()
        .when()
        .get("store/99999")
        .then()
        .statusCode(404);
  }

  @Test
  public void create_shouldReturn422WithStructuredErrorWhenIdSetOnRequest() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "id": 1,
              "name": "BAD_STORE",
              "quantityProductsInStock": 0
            }
            """)
        .when()
        .post("store")
        .then()
        .statusCode(422)
        .body(containsString("status"))
        .body(containsString("message"));
  }

  @Test
  public void update_shouldReturn404WhenStoreNotFound() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "UPDATED",
              "quantityProductsInStock": 0
            }
            """)
        .when()
        .put("store/99999")
        .then()
        .statusCode(404);
  }

  @Test
  public void delete_shouldReturn404WhenStoreNotFound() {
    given()
        .when()
        .delete("store/99999")
        .then()
        .statusCode(404);
  }

  @Test
  public void errorResponse_shouldHaveStructuredFormatWithStatusAndMessage() {
    given()
        .when()
        .get("store/99999")
        .then()
        .statusCode(404)
        .body(containsString("status"))
        .body(containsString("message"))
        .body(containsString("traceId"));
  }
}

