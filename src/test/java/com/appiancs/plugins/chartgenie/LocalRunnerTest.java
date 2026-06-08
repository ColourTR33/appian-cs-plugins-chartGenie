package com.appiancs.plugins.chartgenie;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.appiancs.plugins.chartgenie.dto.structure.ReportRequest;
import com.appiancs.plugins.chartgenie.service.WordDocumentService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * JUnit 5 test replacing the manual LocalRunner.
 * Generates a report from a template and JSON payload,
 * verifying that the output is produced successfully.
 */
class LocalRunnerTest {

  private static final Path TEST_DIR = Paths.get("src/test/java/com/appiancs/plugins/chartgenie");
  private static final Path TEMPLATE_PATH = TEST_DIR.resolve("template.docx");
  private static final Path PAYLOAD_PATH = TEST_DIR.resolve("payload.json");

  @Test
  void generateReport_fromTemplateAndPayload() throws Exception {
    Assumptions.assumeTrue(Files.exists(TEMPLATE_PATH),
        "Template file not available in CI environment");
    Assumptions.assumeTrue(Files.exists(PAYLOAD_PATH),
        "Payload file not available in CI environment");

    // WordDocumentService requires Appian runtime classes that may not be
    // available outside the plugin container (stub JAR lacks method bodies).
    WordDocumentService service;
    try {
      service = new WordDocumentService();
    } catch (NoClassDefFoundError | ClassFormatError e) {
      Assumptions.abort("Appian runtime classes not available: " + e.getMessage());
      return;
    }

    String json = new String(Files.readAllBytes(PAYLOAD_PATH), "UTF-8");

    Gson gson = new Gson();
    Type type = new TypeToken<ReportRequest>() {}.getType();
    ReportRequest req = gson.fromJson(json, type);
    assertNotNull(req, "Parsed ReportRequest should not be null");
    assertNotNull(req.getSections(), "ReportRequest sections should not be null");

    byte[] result = service.generateReport(
        TEMPLATE_PATH.toFile(), req.getSettings(), req.getSections());

    assertNotNull(result, "Generated report byte array should not be null");
    assertTrue(result.length > 0, "Generated report should not be empty");
  }

  @Test
  void parsePayload_successfully() throws Exception {
    Assumptions.assumeTrue(Files.exists(PAYLOAD_PATH),
        "Payload file not available in CI environment");

    String json = new String(Files.readAllBytes(PAYLOAD_PATH), "UTF-8");

    Gson gson = new Gson();
    Type type = new TypeToken<ReportRequest>() {}.getType();
    ReportRequest req = gson.fromJson(json, type);

    assertNotNull(req, "ReportRequest should be parsed from payload.json");
    assertNotNull(req.getSettings(), "ReportSettings should not be null");
    assertNotNull(req.getSections(), "ReportSections should not be null");
    assertTrue(req.getSections().size() > 0, "ReportRequest should contain at least one section");
  }
}
