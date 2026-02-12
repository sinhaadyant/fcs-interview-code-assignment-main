package com.fulfilment.application.monolith.common;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Generates a unique request ID (traceId), sets request path and language from Accept-Language,
 * and stores them in RequestContext for logging and error responses.
 */
@Provider
public class RequestContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final Logger LOGGER = Logger.getLogger(RequestContextFilter.class);
  private static final String REQUEST_ID_HEADER = "X-Request-Id";

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String requestId = requestContext.getHeaderString(REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }

    String path = null;
    UriInfo uriInfo = requestContext.getUriInfo();
    if (uriInfo != null && uriInfo.getPath() != null) {
      String p = uriInfo.getPath();
      path = p.startsWith("/") ? p : "/" + p;
    }

    String language = "en";
    List<String> acceptLanguage = requestContext.getHeaders().get("Accept-Language");
    if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
      String raw = acceptLanguage.get(0);
      if (raw != null && !raw.isBlank()) {
        if (raw.toLowerCase().startsWith("hi")) {
          language = "hi";
        } else {
          language = Locale.forLanguageTag(raw.split(",")[0].trim()).getLanguage();
          if (language == null || language.isBlank()) language = "en";
        }
      }
    }

    RequestContext.set(requestId, path, language);
    LOGGER.debugf("Request started requestId=%s path=%s language=%s", requestId, path, language);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    long processingMs = RequestContext.processingTimeMs();
    LOGGER.infof("Request completed requestId=%s path=%s status=%s processingTimeMs=%d",
        RequestContext.getRequestId(),
        RequestContext.getPath(),
        responseContext.getStatus(),
        processingMs);
    RequestContext.clear();
  }
}
