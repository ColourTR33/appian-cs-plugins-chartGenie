package com.appiancs.plugins.chartgenie;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.appiancs.plugins.chartgenie.dto.structure.ReportRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * JUnit 5 test that validates all JSON payload files parse correctly into ReportRequest objects.
 * Replaces the former manual JsonRunner test runner.
 */
class JsonRunnerTest {

  private static final String TEST_DIR = "src/test/java/com/appiancs/plugins/chartgenie/";
  private static final Gson GSON = new Gson();
  private static final Type REQUEST_TYPE = new TypeToken<ReportRequest>() {}.getType();

  @Test
  void parseBiaFullPayload() throws Exception {
    assertPayloadParsesCorrectly(Path.of(TEST_DIR + "bia-full-payload.json"));
  }

  @Test
  void parseAllComponentsPayload() throws Exception {
    assertPayloadParsesCorrectly(Path.of(TEST_DIR + "all-components-payload.json"));
  }

  @Test
  void parseHtmlMarkupPayload() throws Exception {
    assertPayloadParsesCorrectly(Path.of(TEST_DIR + "html-markup-payload.json"));
  }

  @Test
  void parseNestedTablesPayload() throws Exception {
    assertPayloadParsesCorrectly(Path.of(TEST_DIR + "nested-tables-payload.json"));
  }

  @Test
  void parseScatterPayload() throws Exception {
    assertPayloadParsesCorrectly(Path.of(TEST_DIR + "scatter-payload.json"));
  }

  @Test
  void parseSecurityTestPayload() throws Exception {
    assertPayloadParsesCorrectly(Path.of(TEST_DIR + "security-test-payload.json"));
  }

  @Test
  void parseTestDirPayload() throws Exception {
    assertPayloadParsesCorrectly(Path.of(TEST_DIR + "payload.json"));
  }

  @Test
  void parseProjectRootPayload() throws Exception {
    assertPayloadParsesCorrectly(Path.of("payload.json"));
  }

  @Test
  void parseExamplesFullJson() throws Exception {
    assertPayloadParsesCorrectly(Path.of("examples/fulljson.json"));
  }

  @Test
  void parseExamplesMinimalJson() throws Exception {
    assertPayloadParsesCorrectly(Path.of("examples/minimaljson.json"));
  }

  private void assertPayloadParsesCorrectly(Path jsonPath) throws Exception {
    Assumptions.assumeTrue(Files.exists(jsonPath),
        "JSON payload not available: " + jsonPath);

    String json = Files.readString(jsonPath);
    ReportRequest request = GSON.fromJson(json, REQUEST_TYPE);

    assertNotNull(request, "ReportRequest should not be null for: " + jsonPath);
    assertNotNull(request.getSettings(), "ReportRequest should have settings for: " + jsonPath);
    assertNotNull(request.getSections(), "ReportRequest should have sections for: " + jsonPath);
    assertFalse(request.getSections().isEmpty(),
        "ReportRequest should have at least one section for: " + jsonPath);
  }
}
