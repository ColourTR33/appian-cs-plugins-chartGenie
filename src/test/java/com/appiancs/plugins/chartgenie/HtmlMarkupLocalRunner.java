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

public class HtmlMarkupLocalRunner {

  private static final Logger logger = LoggerFactory.getLogger(HtmlMarkupLocalRunner.class);

  public static void main(String[] args) {
    try {
      logger.info("--- Starting HTML Markup Rendering Test ---");

      String json = new String(Files.readAllBytes(Paths.get("html-markup-payload.json")), "UTF-8");

      Gson gson = new Gson();
      Type type = new TypeToken<ReportRequest>() {
      }.getType();
      ReportRequest req = gson.fromJson(json, type);

      WordDocumentService service = new WordDocumentService();
      byte[] result = service.generateReport(new File("template.docx"), req.getSettings(), req.getSections());

      File outputFile = new File("html-markup-report.docx");
      try (FileOutputStream fos = new FileOutputStream(outputFile)) {
        fos.write(result);
      }

      logger.info("HTML Markup Report Generated: {} ({} bytes)", outputFile.getAbsolutePath(), result.length);
      Desktop.getDesktop().open(outputFile);

    } catch (Exception e) {
      logger.error("HTML markup report generation failed", e);
    }
  }
}
