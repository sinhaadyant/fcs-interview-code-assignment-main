package com.fulfilment.application.monolith.common;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Thread-local request context for the current request: requestId (traceId), path, language, start time.
 * Populated by RequestContextFilter; used by exception mappers and response builders.
 */
@ApplicationScoped
public class RequestContext {

  private static final ThreadLocal<ContextData> HOLDER = new ThreadLocal<>();

  public static void set(String requestId, String path, String language) {
    HOLDER.set(new ContextData(requestId, path, language, System.currentTimeMillis()));
  }

  public static ContextData get() {
    return HOLDER.get();
  }

  public static void clear() {
    HOLDER.remove();
  }

  public static String getRequestId() {
    ContextData d = HOLDER.get();
    return d != null ? d.requestId : null;
  }

  public static String getPath() {
    ContextData d = HOLDER.get();
    return d != null ? d.path : null;
  }

  public static String getLanguage() {
    ContextData d = HOLDER.get();
    return d != null ? d.language : "en";
  }

  public static long getStartTime() {
    ContextData d = HOLDER.get();
    return d != null ? d.startTimeMillis : System.currentTimeMillis();
  }

  public static long processingTimeMs() {
    return System.currentTimeMillis() - getStartTime();
  }

  public static class ContextData {
    public final String requestId;
    public final String path;
    public final String language;
    public final long startTimeMillis;

    public ContextData(String requestId, String path, String language, long startTimeMillis) {
      this.requestId = requestId;
      this.path = path;
      this.language = language;
      this.startTimeMillis = startTimeMillis;
    }
  }
}
