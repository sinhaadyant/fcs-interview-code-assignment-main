package com.fulfilment.application.monolith.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RequestContextTest {

  @AfterEach
  void tearDown() {
    RequestContext.clear();
  }

  @Test
  void set_andGet_returnStoredValues() {
    RequestContext.set("req-1", "/store", "hi");
    assertEquals("req-1", RequestContext.getRequestId());
    assertEquals("/store", RequestContext.getPath());
    assertEquals("hi", RequestContext.getLanguage());
    RequestContext.clear();
    assertNull(RequestContext.getRequestId());
    assertEquals("en", RequestContext.getLanguage()); // default after clear
  }

  @Test
  void getLanguage_returnsDefaultWhenCleared() {
    RequestContext.clear();
    assertEquals("en", RequestContext.getLanguage());
  }

  @Test
  void processingTimeMs_increasesAfterSet() {
    RequestContext.set("r", "/", "en");
    long t0 = RequestContext.processingTimeMs();
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    long t1 = RequestContext.processingTimeMs();
    assert t1 >= t0 : "processingTimeMs should increase over time";
  }
}
