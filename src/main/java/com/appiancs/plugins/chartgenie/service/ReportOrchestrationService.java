package com.appiancs.plugins.chartgenie.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.appiancs.plugins.chartgenie.dto.structure.ReportRequest;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

public class ReportOrchestrationService {

  private final ChartGenerationService chartService = new ChartGenerationService();
  private final WordDocumentService wordService = new WordDocumentService();

  public File generateReport(ReportRequest request) throws Exception {
    System.out.println("--- Orchestrator: Starting Report Generation ---");

    // 1. Initialize the Base Document
    // (In the future, we can read request.getDocumentSettings() to pick templates)
    File currentDoc = wordService.createBaseDocument();

    // 2. Loop through the JSON "Content" list
    for (ReportSection section : request.getContent()) {

      switch (section.getType()) {
        case HEADING:
          System.out.println("   > Processing: HEADING");
          currentDoc = wordService.appendTextToDocument(currentDoc, section.getText(), true);
          break;

        case PARAGRAPH:
          System.out.println("   > Processing: PARAGRAPH");
          currentDoc = wordService.appendTextToDocument(currentDoc, section.getText(), false);
          break;

        case CHART:
          System.out.println("   > Processing: CHART");
          if (section.getChartConfig() != null) {
            File chartImage = chartService.generateChartImage(section.getChartConfig());
            currentDoc = wordService.appendChartToDocument(currentDoc, chartImage);
          }
          break;

        case TABLE:
          System.out.println("   > Processing: TABLE");
          List<String[]> tableData = convertToTableData(section.getTableHeaders(), section.getTableRows());
          currentDoc = wordService.appendTableToDocument(currentDoc, tableData);
          break;
      }
    }

    System.out.println("--- Orchestrator: Finished ---");
    return currentDoc;
  }

  /**
   * Helper to convert the DTO Lists into the String Arrays required by WordDocumentService
   */
  private List<String[]> convertToTableData(List<String> headers, List<List<String>> rows) {
    List<String[]> result = new ArrayList<>();

    // Add Headers
    if (headers != null && !headers.isEmpty()) {
      result.add(headers.toArray(new String[0]));
    }

    // Add Rows
    if (rows != null) {
      for (List<String> row : rows) {
        result.add(row.toArray(new String[0]));
      }
    }
    return result;
  }
}