package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    // given
    LocationGateway locationGateway = new LocationGateway();

    // when
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    // then
    assertEquals("ZWOLLE-001", location.identification);
  }

  @Test
  public void testWhenResolveUnknownLocationShouldReturnNull() {
    LocationGateway locationGateway = new LocationGateway();

    Location location = locationGateway.resolveByIdentifier("UNKNOWN-LOCATION");

    assertNull(location);
  }

  @Test
  public void testWhenResolveNullIdentifierShouldReturnNull() {
    LocationGateway locationGateway = new LocationGateway();
    assertNull(locationGateway.resolveByIdentifier(null));
  }
}
