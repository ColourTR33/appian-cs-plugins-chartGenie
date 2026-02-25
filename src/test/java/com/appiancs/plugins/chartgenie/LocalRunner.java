package com.appiancs.plugins.chartgenie;

// --- FIX: Import specifically from 'dto.structure' ---
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.appiancs.plugins.chartgenie.dto.structure.ReportRequest;
import com.appiancs.plugins.chartgenie.service.WordDocumentService;
import com.google.gson.Gson;

public class LocalRunner {

  public static void main(String[] args) {
    try {
      System.out.println("--- 🚀 Starting Local ChartGenie Test ---");

      String jsonPath = "payload.json";
      String templatePath = "template.docx";

      File jsonFile = new File(jsonPath);
      if (!jsonFile.exists()) {
        // Fallback for different working directories
        jsonFile = new File("../payload.json");
        if (jsonFile.exists()) {
          jsonPath = "../payload.json";
          templatePath = "../template.docx";
        } else {
          throw new RuntimeException("❌ Missing 'payload.json'! Please create it in the project root.");
        }
      }

      String jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)));

      Gson gson = new Gson();
      ReportRequest request = gson.fromJson(jsonContent, ReportRequest.class);
      System.out.println("✅ JSON Parsed Successfully");

      File templateFile = new File(templatePath);
      if (!templateFile.exists()) {
        throw new RuntimeException("❌ Missing 'template.docx'!");
      }

      WordDocumentService service = new WordDocumentService();
      File result = service.generateReport(
        templateFile,
        request.getSettings(),
        request.getSections());

      System.out.println("--- 🎉 Success! ---");
      System.out.println("📄 Report generated at: " + result.getAbsolutePath());

      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(result);
      }

    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("--- 💥 Failure ---");
      System.err.println(e.getMessage());
    }
  }
}