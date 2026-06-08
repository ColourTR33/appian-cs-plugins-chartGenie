package com.appiancs.plugins.chartgenie;

import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.appiancs.plugins.chartgenie.dto.structure.ReportRequest;
import com.appiancs.plugins.chartgenie.service.WordDocumentService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ScatterPlotTestRunner {
  public static void main(String[] args) {
    try {
      System.out.println("--- Starting Scatter Plot Test ---");

      // Use the scatter plot test payload
      String json = new String(Files.readAllBytes(Paths.get("scatter-payload.json")), "UTF-8");

      Gson gson = new Gson();
      Type type = new TypeToken<ReportRequest>() {
      }.getType();
      ReportRequest req = gson.fromJson(json, type);

      WordDocumentService service = new WordDocumentService();
      byte[] result = service.generateReport(new File("template.docx"), req.getSettings(), req.getSections());

      // Write the result to a file
      File outputFile = new File("scatter-plot-report-v3.docx");
      try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile)) {
        fos.write(result);
      }

      System.out.println("Scatter Plot Report Generated: " + outputFile.getAbsolutePath());

      // Open the generated document
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(outputFile);
      }

      System.out.println("--- Scatter Plot Test Completed Successfully ---");
    } catch (Exception e) {
      System.err.println("Error during scatter plot test:");
      e.printStackTrace();
    }
  }
}