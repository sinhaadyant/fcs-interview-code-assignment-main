package com.fulfilment.application.monolith.common;

import com.fulfilment.application.monolith.common.api.ErrorDetail;
import com.fulfilment.application.monolith.common.api.StructuredErrorResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Production-grade global exception handling. Returns structured error response with
 * timestamp, status, error, message, path, errorCode, traceId, details. Never exposes stack trace.
 */
@Provider
public class GlobalApiExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(GlobalApiExceptionMapper.class);
  private static final String ERR_BAD_REQUEST = "error.bad_request";
  private static final String ERR_NOT_FOUND = "error.not_found";
  private static final String ERR_VALIDATION = "error.validation_failed";
  private static final String ERR_INTERNAL = "error.internal";
  private static final String ERROR_CODE_VALIDATION = "VAL_001";
  private static final String ERROR_CODE_BUSINESS = "BIZ_001";
  private static final String ERROR_CODE_NOT_FOUND = "NF_001";
  private static final String ERROR_CODE_SERVER = "SRV_001";

  @Inject
  MessageService messageService;

  @Override
  public Response toResponse(Exception exception) {
    String traceId = RequestContext.getRequestId();
    String path = RequestContext.getPath();
    String language = RequestContext.getLanguage();

    // Log internally with full stack trace; never expose in response
    LOGGER.errorf(exception, "Request failed traceId=%s path=%s", traceId, path);

    int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    String errorPhrase = messageService.get(ERR_INTERNAL, language);
    String message = messageService.get("generic.error", language);
    String errorCode = ERROR_CODE_SERVER;
    List<ErrorDetail> details = null;

    if (exception instanceof BusinessException be) {
      status = be.getStatus();
      errorCode = be.getErrorCode();
      String key = be.getMessageKey();
      Object[] args = be.getMessageArgs();
      if (key != null) {
        message = args != null && args.length > 0
            ? messageService.get(key, language, args)
            : messageService.get(key, language);
      } else {
        message = be.getMessage();
      }
      errorPhrase = status == 404 ? messageService.get(ERR_NOT_FOUND, language) : messageService.get(ERR_BAD_REQUEST, language);
    } else if (exception instanceof WebApplicationException wae) {
      Response r = wae.getResponse();
      if (r != null) status = r.getStatus();
      message = wae.getMessage() != null ? wae.getMessage() : messageService.get("generic.error", language);
      errorPhrase = status == 404 ? messageService.get(ERR_NOT_FOUND, language)
          : status == 422 ? messageService.get(ERR_VALIDATION, language)
          : messageService.get(ERR_BAD_REQUEST, language);
      errorCode = status == 404 ? ERROR_CODE_NOT_FOUND : ERROR_CODE_VALIDATION;
    } else if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
      status = Response.Status.BAD_REQUEST.getStatusCode();
      errorPhrase = messageService.get(ERR_BAD_REQUEST, language);
      message = exception.getMessage() != null ? exception.getMessage() : messageService.get(ERR_VALIDATION, language);
      errorCode = ERROR_CODE_BUSINESS;
    } else if (isConstraintViolation(exception)) {
      status = Response.Status.BAD_REQUEST.getStatusCode();
      errorPhrase = messageService.get(ERR_BAD_REQUEST, language);
      message = messageService.get(ERR_VALIDATION, language);
      errorCode = ERROR_CODE_VALIDATION;
      details = extractConstraintViolationDetails(exception, language);
    }

    if (message == null || message.isBlank()) {
      message = messageService.get("generic.error", language);
    }

    StructuredErrorResponse body = StructuredErrorResponse.of(
        status,
        errorPhrase,
        message,
        path,
        errorCode,
        traceId,
        details);

    return Response.status(status).entity(body).build();
  }

  private static boolean isConstraintViolation(Exception e) {
    return e.getClass().getName().contains("ConstraintViolation");
  }

  private List<ErrorDetail> extractConstraintViolationDetails(Exception e, String language) {
    try {
      java.lang.reflect.Method m = e.getClass().getMethod("getConstraintViolations");
      @SuppressWarnings("unchecked")
      java.util.Set<?> violations = (java.util.Set<?>) m.invoke(e);
      if (violations == null || violations.isEmpty()) return null;
      return violations.stream()
          .map(v -> {
            String field = getViolationProperty(v, "getPropertyPath");
            Object invalid = getViolationPropertyValue(v, "getInvalidValue");
            String msg = getViolationProperty(v, "getMessage");
            return ErrorDetail.of(field, invalid, msg);
          })
          .collect(Collectors.toList());
    } catch (Exception ex) {
      return null;
    }
  }

  private static String getViolationProperty(Object v, String methodName) {
    try {
      java.lang.reflect.Method m = v.getClass().getMethod(methodName);
      Object o = m.invoke(v);
      return o != null ? o.toString() : null;
    } catch (Exception e) {
      return null;
    }
  }

  private static Object getViolationPropertyValue(Object v, String methodName) {
    try {
      java.lang.reflect.Method m = v.getClass().getMethod(methodName);
      return m.invoke(v);
    } catch (Exception e) {
      return null;
    }
  }
}
