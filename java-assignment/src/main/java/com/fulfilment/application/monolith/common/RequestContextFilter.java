package com.fulfilment.application.monolith.common;

import jakarta.inject.Inject;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Generates a unique request ID (traceId), sets request path and language from Accept-Language
 * (or default locale from config), and stores them in RequestContext. Adds X-Response-Time-Ms
 * on responses. Used for logging and error responses.
 */
@Provider
public class RequestContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final Logger LOGGER = Logger.getLogger(RequestContextFilter.class);
  private static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String RESPONSE_TIME_HEADER = "X-Response-Time-Ms";

  @Inject
  @ConfigProperty(name = "app.default-locale", defaultValue = "en")
  String defaultLocale;

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

    String language = defaultLocale != null && !defaultLocale.isBlank() ? defaultLocale.trim() : "en";
    List<String> acceptLanguage = requestContext.getHeaders().get("Accept-Language");
    if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
      String raw = acceptLanguage.get(0);
      if (raw != null && !raw.isBlank()) {
        String tag = raw.split(",")[0].trim();
        if (tag.toLowerCase().startsWith("hi")) {
          language = "hi";
        } else if (tag.toLowerCase().startsWith("nl")) {
          language = "nl";
        } else {
          String parsed = Locale.forLanguageTag(tag).getLanguage();
          if (parsed != null && !parsed.isBlank()) language = parsed;
        }
      }
    }

    RequestContext.set(requestId, path, language);
    LOGGER.debugf("Request started requestId=%s path=%s language=%s", requestId, path, language);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    long processingMs = RequestContext.processingTimeMs();
    responseContext.getHeaders().putSingle(RESPONSE_TIME_HEADER, processingMs);
    LOGGER.infof("Request completed requestId=%s path=%s status=%s processingTimeMs=%d",
        RequestContext.getRequestId(),
        RequestContext.getPath(),
        responseContext.getStatus(),
        processingMs);
    RequestContext.clear();
  }
}
