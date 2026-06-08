package com.appiancs.plugins.chartgenie;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
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
 * JUnit 5 test replacing the manual NestedTablesLocalRunner.
 * Generates a report with nested table sections from a template and JSON payload,
 * verifying that the output is produced successfully.
 */
class NestedTablesLocalRunnerTest {

  private static final Path TEST_DIR = Paths.get("src/test/java/com/appiancs/plugins/chartgenie");
  private static final Path TEMPLATE_PATH = TEST_DIR.resolve("template.docx");
  private static final Path PAYLOAD_PATH = TEST_DIR.resolve("nested-tables-payload.json");

  @Test
  void generateNestedTablesReport() throws Exception {
    Assumptions.assumeTrue(Files.exists(TEMPLATE_PATH),
        "Template not available in CI environment");
    Assumptions.assumeTrue(Files.exists(PAYLOAD_PATH),
        "Nested tables payload not available in CI environment");

    String json = Files.readString(PAYLOAD_PATH);

    Gson gson = new Gson();
    Type type = new TypeToken<ReportRequest>() {}.getType();
    ReportRequest req = gson.fromJson(json, type);
    assertNotNull(req, "ReportRequest should parse successfully from JSON");
    assertNotNull(req.getSections(), "ReportRequest should contain sections");

    WordDocumentService service = new WordDocumentService();
    byte[] result = service.generateReport(
        new File(TEMPLATE_PATH.toString()), req.getSettings(), req.getSections());

    assertNotNull(result, "Generated nested tables report should not be null");
    assertTrue(result.length > 0, "Generated nested tables report should not be empty");
  }

  @Test
  void parseNestedTablesPayload_successfully() throws Exception {
    Assumptions.assumeTrue(Files.exists(PAYLOAD_PATH),
        "Nested tables payload not available in CI environment");

    String json = Files.readString(PAYLOAD_PATH);

    Gson gson = new Gson();
    Type type = new TypeToken<ReportRequest>() {}.getType();
    ReportRequest req = gson.fromJson(json, type);

    assertNotNull(req, "ReportRequest should be parsed from nested-tables-payload.json");
    assertNotNull(req.getSettings(), "ReportSettings should not be null");
    assertNotNull(req.getSections(), "ReportSections should not be null");
    assertTrue(!req.getSections().isEmpty(), "ReportRequest should contain at least one section");
  }
}
