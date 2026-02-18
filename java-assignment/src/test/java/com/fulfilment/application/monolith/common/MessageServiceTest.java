package com.fulfilment.application.monolith.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MessageServiceTest {

  @Inject MessageService messageService;

  @Test
  void get_returnsEnglish_whenLanguageEn() {
    assertEquals("Store created successfully", messageService.get("store.created", "en"));
    assertEquals("Not Found", messageService.get("error.not_found", "en"));
  }

  @Test
  void get_returnsHindi_whenLanguageHi() {
    assertNotNull(messageService.get("store.created", "hi"));
    assertTrue(messageService.get("store.created", "hi").length() > 0);
  }

  @Test
  void get_returnsDutch_whenLanguageNl() {
    assertEquals("Winkel succesvol aangemaakt", messageService.get("store.created", "nl"));
  }

  @Test
  void get_withArgs_formatsPlaceholder() {
    assertEquals(
        "Store with id of 42 does not exist",
        messageService.get("store.not_found", "en", 42));
  }

  @Test
  void get_returnsKey_whenKeyMissing() {
    assertEquals("missing.key", messageService.get("missing.key", "en"));
  }
}
