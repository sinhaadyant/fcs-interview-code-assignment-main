package com.fulfilment.application.monolith.common;

import jakarta.ws.rs.core.Response;

/**
 * Custom business rule violation. Mapped to 400 (or 404 if not found) with structured error response.
 * Use errorCode for client-side handling (e.g. USR_001).
 */
public class BusinessException extends RuntimeException {

  private final int status;
  private final String errorCode;
  private final String messageKey;

  public BusinessException(String message) {
    super(message);
    this.status = Response.Status.BAD_REQUEST.getStatusCode();
    this.errorCode = "BIZ_001";
    this.messageKey = null;
  }

  public BusinessException(String message, int status, String errorCode) {
    super(message);
    this.status = status;
    this.errorCode = errorCode != null ? errorCode : "BIZ_001";
    this.messageKey = null;
  }

  public BusinessException(String messageKey, int status, String errorCode, String defaultMessage) {
    super(defaultMessage);
    this.status = status;
    this.errorCode = errorCode != null ? errorCode : "BIZ_001";
    this.messageKey = messageKey;
  }

  public int getStatus() {
    return status;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getMessageKey() {
    return messageKey;
  }
}
