package com.fulfilment.application.monolith.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.List;

/**
 * Production-grade structured error response. Never exposes stack trace.
 * Aligns with enterprise API standards (timestamp, status, error, message, path, errorCode, traceId, details).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "timestamp",
  "status",
  "error",
  "message",
  "path",
  "errorCode",
  "traceId",
  "details"
})
public class StructuredErrorResponse {

  public String timestamp;
  public int status;
  public String error;
  public String message;
  public String path;
  public String errorCode;
  public String traceId;
  public List<ErrorDetail> details;

  public StructuredErrorResponse() {}

  public StructuredErrorResponse(
      String timestamp,
      int status,
      String error,
      String message,
      String path,
      String errorCode,
      String traceId,
      List<ErrorDetail> details) {
    this.timestamp = timestamp;
    this.status = status;
    this.error = error;
    this.message = message;
    this.path = path;
    this.errorCode = errorCode;
    this.traceId = traceId;
    this.details = details;
  }

  public static StructuredErrorResponse of(
      int status,
      String error,
      String message,
      String path,
      String errorCode,
      String traceId,
      List<ErrorDetail> details) {
    return new StructuredErrorResponse(
        Instant.now().toString(),
        status,
        error,
        message,
        path,
        errorCode,
        traceId,
        details);
  }
}
