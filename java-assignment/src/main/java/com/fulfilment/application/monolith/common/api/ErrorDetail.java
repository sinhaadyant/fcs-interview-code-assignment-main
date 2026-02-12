package com.fulfilment.application.monolith.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Single validation or constraint error (field-level). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetail {

  public String field;
  public Object rejectedValue;
  public String message;

  public ErrorDetail() {}

  public ErrorDetail(String field, Object rejectedValue, String message) {
    this.field = field;
    this.rejectedValue = rejectedValue;
    this.message = message;
  }

  public static ErrorDetail of(String field, Object rejectedValue, String message) {
    return new ErrorDetail(field, rejectedValue, message);
  }
}
