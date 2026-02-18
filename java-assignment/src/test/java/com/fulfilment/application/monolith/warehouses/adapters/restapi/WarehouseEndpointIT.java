package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
public class WarehouseEndpointIT {

  @Test
  public void testSimpleListWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testArchivingAndReplacingWarehouses() {
    final String path = "warehouse";

    // List all, should have all 3 warehouses initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Archive the warehouse with id 1 (ZWOLLE-001)
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, ZWOLLE-001 should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString("ZWOLLE-001")),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Replace warehouse MWH.012 with a new warehouse at the same location
    given()
        .contentType("application/json")
        .body(
            """
            {
              "location": "AMSTERDAM-001",
              "capacity": 60,
              "stock": 5
            }
            """)
        .when()
        .post(path + "/MWH.012/replacement")
        .then()
        .statusCode(200)
        .body(containsString("AMSTERDAM-001"), containsString("60"));
  }

  @Test
  public void testFulfilmentAssign() {
    // Assign warehouse MWH.001 to product 1 for store 1 -> 201
    given()
        .contentType("application/json")
        .body("{\"warehouseBusinessUnitCode\": \"MWH.001\", \"productId\": 1}")
        .when()
        .post("store/1/fulfilment")
        .then()
        .statusCode(201)
        .header("X-Message", containsString("assigned"));

    // Idempotent: same assignment again -> 201
    given()
        .contentType("application/json")
        .body("{\"warehouseBusinessUnitCode\": \"MWH.001\", \"productId\": 1}")
        .when()
        .post("store/1/fulfilment")
        .then()
        .statusCode(201);

    // Invalid warehouse -> 400
    given()
        .contentType("application/json")
        .body("{\"warehouseBusinessUnitCode\": \"INVALID\", \"productId\": 1}")
        .when()
        .post("store/1/fulfilment")
        .then()
        .statusCode(400);
  }
}
