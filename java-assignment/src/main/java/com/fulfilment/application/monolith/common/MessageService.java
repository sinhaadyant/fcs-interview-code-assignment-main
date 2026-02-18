package com.fulfilment.application.monolith.common;

import jakarta.enterprise.context.ApplicationScoped;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * i18n message resolution. Uses Accept-Language (via RequestContext) or default locale from config.
 * Loads from messages_en.properties, messages_hi.properties, messages_nl.properties.
 */
@ApplicationScoped
public class MessageService {

  private static final String BASENAME = "messages";
  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
  private static final Locale HINDI = Locale.forLanguageTag("hi");
  private static final Locale DUTCH = Locale.forLanguageTag("nl");

  public String get(String key) {
    return get(key, RequestContext.getLanguage());
  }

  public String get(String key, String language) {
    Locale locale = toLocale(language);
    try {
      ResourceBundle bundle = ResourceBundle.getBundle(BASENAME, locale);
      return bundle.containsKey(key) ? bundle.getString(key) : key;
    } catch (Exception e) {
      return key;
    }
  }

  private static Locale toLocale(String language) {
    if (language == null || language.isBlank()) return DEFAULT_LOCALE;
    return switch (language.toLowerCase()) {
      case "hi" -> HINDI;
      case "nl" -> DUTCH;
      default -> Locale.forLanguageTag(language).getLanguage().isBlank() ? DEFAULT_LOCALE : Locale.forLanguageTag(language);
    };
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
