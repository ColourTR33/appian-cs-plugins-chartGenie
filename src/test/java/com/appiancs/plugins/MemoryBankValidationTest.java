package com.appiancs.plugins;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates Memory Bank Documentation completeness
 */
public class MemoryBankValidationTest {

  private static final Logger logger = LoggerFactory.getLogger(MemoryBankValidationTest.class);
  private static final String MEMORY_BANK_PATH = "c:\\Repos\\appian-plugins\\appian-cs-plugins\\chartGenie\\memory-bank";

  @Test
  public void validateAllMemoryBankFiles() {
    logger.info("Validating Memory Bank documentation files");

    // Check if memory bank directory exists
    Path memoryBankDir = Paths.get(MEMORY_BANK_PATH);
    assertTrue(Files.exists(memoryBankDir), "Memory bank directory should exist");

    // Validate required files
    validateFile("product.md", "Product overview documentation");
    validateFile("structure.md", "Project structure documentation");
    validateFile("tech.md", "Technical architecture documentation");
    validateFile("guidelines.md", "Development guidelines documentation");

    logger.info("✓ All Memory Bank files validated successfully");
  }

  private void validateFile(String filename, String description) {
    Path filePath = Paths.get(MEMORY_BANK_PATH, filename);
    assertTrue(Files.exists(filePath), description + " should exist: " + filename);

    try {
      String content = Files.readString(filePath);
      assertFalse(content.trim().isEmpty(), description + " should not be empty: " + filename);
      assertTrue(content.length() > 100, description + " should have substantial content: " + filename);
      logger.info("✓ Validated {}: {} characters", filename, content.length());
    } catch (Exception e) {
      fail("Failed to read " + filename + ": " + e.getMessage());
    }
  }
}