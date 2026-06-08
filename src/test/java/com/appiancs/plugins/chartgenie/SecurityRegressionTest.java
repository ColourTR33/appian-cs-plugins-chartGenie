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

public class SecurityRegressionTest {
  public static void main(String[] args) {
    try {
      System.out.println("--- Starting Security Regression Test ---");

      // Use the security test payload
      String json = new String(Files.readAllBytes(Paths.get("security-test-payload.json")), "UTF-8");

      Gson gson = new Gson();
      Type type = new TypeToken<ReportRequest>() {
      }.getType();
      ReportRequest req = gson.fromJson(json, type);

      WordDocumentService service = new WordDocumentService();
      byte[] result = service.generateReport(new File("template.docx"), req.getSettings(), req.getSections());

      // Write the result to a file
      File outputFile = new File("security-regression-test.docx");
      try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile)) {
        fos.write(result);
      }

      System.out.println("Security Regression Test Report Generated: " + outputFile.getAbsolutePath());

      // Open the generated document
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(outputFile);
      }

      System.out.println("--- Security Regression Test Completed Successfully ---");
    } catch (Exception e) {
      System.err.println("Error during security regression test:");
      e.printStackTrace();
    }
  }
}