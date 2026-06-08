package com.appiancs.plugins.chartgenie;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.appiancs.plugins.chartgenie.dto.structure.ReportRequest;
import com.appiancs.plugins.chartgenie.service.WordDocumentService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

class ScatterPlotTestRunnerTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src/test/java/com/appiancs/plugins/chartgenie/template.docx");
  private static final Path PAYLOAD_PATH =
      Path.of("src/test/java/com/appiancs/plugins/chartgenie/scatter-payload.json");

  @Test
  void generateScatterPlotReport() throws Exception {
    Assumptions.assumeTrue(Files.exists(TEMPLATE_PATH),
        "Template not available in CI environment");
    Assumptions.assumeTrue(Files.exists(PAYLOAD_PATH),
        "Scatter plot payload not available in CI environment");

    String json = Files.readString(PAYLOAD_PATH);

    Gson gson = new Gson();
    Type type = new TypeToken<ReportRequest>() {}.getType();
    ReportRequest req = gson.fromJson(json, type);
    assertNotNull(req, "ReportRequest should parse successfully from JSON");
    assertNotNull(req.getSections(), "ReportRequest should contain sections");

    WordDocumentService service = new WordDocumentService();
    byte[] result = service.generateReport(
        new File(TEMPLATE_PATH.toString()), req.getSettings(), req.getSections());

    assertNotNull(result, "Generated scatter plot report should not be null");
    assertTrue(result.length > 0, "Generated scatter plot report should not be empty");
  }
}
