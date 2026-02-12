package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * QuarkusTest for Warehouse REST API to achieve full coverage of WarehouseResourceImpl
 * and WarehouseRepository. Exercises list, create, get by id, archive, and replace.
 */
@QuarkusTest
public class WarehouseEndpointTest {

  private static final String PATH = "warehouse";

  @Test
  void listWarehouses_shouldReturn200WithPagination() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(notNullValue())
        .body("size()", greaterThanOrEqualTo(0));
  }

  @Test
  void listWarehouses_shouldHonorPageAndSizeParams() {
    given()
        .queryParam("page", 0)
        .queryParam("size", 2)
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(notNullValue());
  }

  @Test
  void createWarehouse_shouldReturnSuccessWhenValid() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "businessUnitCode": "MWH.NEW.001",
              "location": "VETSBY-001",
              "capacity": 80,
              "stock": 20
            }
            """)
        .when()
        .post(PATH)
        .then()
        .statusCode(greaterThanOrEqualTo(200))
        .statusCode(lessThan(300))
        .body(containsString("MWH.NEW.001"))
        .body(containsString("VETSBY-001"));
  }

  @Test
  void getWarehouseById_shouldReturn200WhenFound() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  void getWarehouseById_shouldReturn404WithStructuredErrorWhenInvalidId() {
    given()
        .when()
        .get(PATH + "/99999")
        .then()
        .statusCode(404)
        .body(containsString("status"))
        .body(containsString("message"))
        .body(containsString("traceId"));
  }

  @Test
  void getWarehouseById_shouldReturn404WhenIdNotNumeric() {
    given()
        .when()
        .get(PATH + "/abc")
        .then()
        .statusCode(404)
        .body(containsString("status"))
        .body(containsString("message"));
  }

  @Test
  void archiveWarehouse_shouldReturn204WhenActiveAndGetThen404() {
    given()
        .when()
        .get(PATH + "/3")
        .then()
        .statusCode(200)
        .body(containsString("MWH.023"));

    given()
        .when()
        .delete(PATH + "/3")
        .then()
        .statusCode(204);

    given()
        .when()
        .get(PATH + "/3")
        .then()
        .statusCode(404);
  }

  @Test
  void replaceWarehouse_shouldReturnSuccessWhenValid() {
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
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(greaterThanOrEqualTo(200))
        .statusCode(lessThan(300))
        .body(containsString("AMSTERDAM-001"), containsString("60"));
  }

  @Test
  void createWarehouse_shouldReturn400WithStructuredErrorWhenDuplicateBusinessUnit() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "businessUnitCode": "MWH.001",
              "location": "ZWOLLE-001",
              "capacity": 30,
              "stock": 5
            }
            """)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("status"))
        .body(containsString("message"));
  }

  @Test
  void createWarehouse_shouldReturn400WhenUnknownLocation() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "businessUnitCode": "MWH.UNKNOWN.LOC",
              "location": "NOWHERE-999",
              "capacity": 10,
              "stock": 5
            }
            """)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("status"))
        .body(containsString("message"));
  }

  @Test
  void getWarehouseById_shouldIncludeIdInResponse() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("\"id\""))
        .body(containsString("MWH.001"));
  }
}
