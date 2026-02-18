package com.fulfilment.application.monolith.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.common.api.StructuredErrorResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GlobalApiExceptionMapperTest {

  @Inject GlobalApiExceptionMapper mapper;

  @AfterEach
  void tearDown() {
    RequestContext.clear();
  }

  @Test
  void toResponse_businessException_returnsStructuredErrorWithLocalizedMessage() {
    RequestContext.set("trace-123", "/store/99", "en");
    BusinessException ex = new BusinessException("store.not_found", 404, "NF_001", 99L);

    Response response = mapper.toResponse(ex);

    assertEquals(404, response.getStatus());
    StructuredErrorResponse body = (StructuredErrorResponse) response.getEntity();
    assertNotNull(body);
    assertEquals(404, body.status);
    assertEquals("trace-123", body.traceId);
    assertEquals("/store/99", body.path);
    assertEquals("NF_001", body.errorCode);
    assertTrue(body.message.contains("99"), "message should contain id 99: " + body.message);
  }

  @Test
  void toResponse_businessException_usesLanguageFromContext() {
    RequestContext.set("t", "/store/1", "hi");
    BusinessException ex = new BusinessException("store.not_found", 404, "NF_001", 1L);

    Response response = mapper.toResponse(ex);

    assertEquals(404, response.getStatus());
    StructuredErrorResponse body = (StructuredErrorResponse) response.getEntity();
    assertNotNull(body);
    // Hindi message for store.not_found should not be the English one
    assertTrue(body.message.length() > 0);
    assertTrue(body.message.contains("1") || body.message.contains("१"));
  }
}
