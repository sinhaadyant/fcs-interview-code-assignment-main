package com.fulfilment.application.monolith.common;

import jakarta.enterprise.context.ApplicationScoped;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * i18n message resolution. Uses Accept-Language (via RequestContext) or default English.
 * Loads from messages_en.properties and messages_hi.properties.
 */
@ApplicationScoped
public class MessageService {

  private static final String BASENAME = "messages";
  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
  private static final Locale HINDI = new Locale("hi");

  public String get(String key) {
    return get(key, RequestContext.getLanguage());
  }

  public String get(String key, String language) {
    Locale locale = "hi".equalsIgnoreCase(language) ? HINDI : DEFAULT_LOCALE;
    try {
      ResourceBundle bundle = ResourceBundle.getBundle(BASENAME, locale);
      return bundle.containsKey(key) ? bundle.getString(key) : key;
    } catch (Exception e) {
      return key;
    }
  }

  public String get(String key, Object... args) {
    String template = get(key);
    return args != null && args.length > 0 ? MessageFormat.format(template, args) : template;
  }

  public String get(String key, String language, Object... args) {
    String template = get(key, language);
    return args != null && args.length > 0 ? MessageFormat.format(template, args) : template;
  }
}
