package com.appiancs.plugins.chartgenie;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.structure.ReportRequest;
import com.appiancs.plugins.chartgenie.service.WordDocumentService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AllComponentsLocalRunner {

  private static final Logger logger = LoggerFactory.getLogger(AllComponentsLocalRunner.class);

  public static void main(String[] args) {
    try {
      logger.info("--- Starting All Components Showcase Test ---");

      String json = new String(Files.readAllBytes(Paths.get("all-components-payload.json")), "UTF-8");

      Gson gson = new Gson();
      Type type = new TypeToken<ReportRequest>() {
      }.getType();
      ReportRequest req = gson.fromJson(json, type);

      WordDocumentService service = new WordDocumentService();
      byte[] result = service.generateReport(new File("template.docx"), req.getSettings(), req.getSections());

      File outputFile = new File("all-components-report.docx");
      try (FileOutputStream fos = new FileOutputStream(outputFile)) {
        fos.write(result);
      }

      logger.info("All Components Report Generated: {} ({} bytes)", outputFile.getAbsolutePath(), result.length);
      Desktop.getDesktop().open(outputFile);

    } catch (Exception e) {
      logger.error("All components report generation failed", e);
    }
  }
}
