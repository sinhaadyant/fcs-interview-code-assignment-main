package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Create a new product:
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "NEW_PRODUCT",
              "description": "Test product",
              "price": 9.99,
              "stock": 2
            }
            """)
        .when()
        .post(path)
        .then()
        .statusCode(201)
        .body(containsString("NEW_PRODUCT"));

    // Update the newly created product (id 4 after import.sql):
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "NEW_PRODUCT_UPDATED",
              "description": "Updated description",
              "price": 19.99,
              "stock": 5
            }
            """)
        .when()
        .put(path + "/4")
        .then()
        .statusCode(200)
        .body(containsString("NEW_PRODUCT_UPDATED"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now and updated product present:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString("TONSTAD")),
            containsString("KALLAX"),
            containsString("BESTÅ"),
            containsString("NEW_PRODUCT_UPDATED"));
  }
}
