package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.logging.Logger;

/**
 * Very small gateway that emulates an integration with a legacy store management system.
 *
 * <p>For the purposes of the assignment we do not actually talk to another system – we write a
 * temporary file, log its contents and delete it again. The important bit is that callers can
 * treat this as "the legacy side effect" that should only run after the primary transaction
 * succeeds.
 */
@ApplicationScoped
public class LegacyStoreManagerGateway {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreManagerGateway.class);

  public void createStoreOnLegacySystem(Store store) {
    // In a real system this would be an HTTP call, message send, etc.
    // Here we just simulate an external side effect via a temporary file.
    writeToFile(store);
  }

  public void updateStoreOnLegacySystem(Store store) {
    // Same simulation as in create: external side effect via a temporary file.
    writeToFile(store);
  }

  private void writeToFile(Store store) {
    try {
      // Step 1: Create a temporary file
      Path tempFile;

      tempFile = Files.createTempFile(store.name, ".txt");
      LOGGER.infov("Temporary file created at: {0}", tempFile);

      // Step 2: Write data to the temporary file
      String content =
          "Store created. [ name ="
              + store.name
              + " ] [ items on stock ="
              + store.quantityProductsInStock
              + "]";
      Files.write(tempFile, content.getBytes());
      LOGGER.infov("Data written to temporary file for store {0}", store.name);

      // Step 3: Optionally, read the data back to verify
      String readContent = new String(Files.readAllBytes(tempFile));
      LOGGER.infov("Data read from temporary file: {0}", readContent);

      // Step 4: Delete the temporary file when done
      Files.delete(tempFile);
      LOGGER.infov("Temporary file deleted: {0}", tempFile);

    } catch (Exception e) {
      LOGGER.error("Error while writing store data to temporary file", e);
    }
  }
}
