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

public class LocalRunner {
  public static void main(String[] args) {
    try {
      System.out.println("--- Starting Local Test ---");
      String json = new String(Files.readAllBytes(Paths.get("payload.json")), "UTF-8");

      Gson gson = new Gson();
      Type type = new TypeToken<ReportRequest>() {
      }.getType();
      ReportRequest req = gson.fromJson(json, type);

      WordDocumentService service = new WordDocumentService();
      File result = service.generateReport(new File("template.docx"), req.getSettings(), req.getSections());

      System.out.println("Report Generated: " + result.getAbsolutePath());
      Desktop.getDesktop().open(result);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}