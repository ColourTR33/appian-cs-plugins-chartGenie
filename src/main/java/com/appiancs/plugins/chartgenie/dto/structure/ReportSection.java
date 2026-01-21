package com.appiancs.plugins.chartgenie.dto.structure;

import java.util.List;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;

public class ReportSection {

  public enum SectionType {
      HEADING, PARAGRAPH, CHART, TABLE
  }

  private SectionType type;

  // Content fields (used based on type)
  private String text;
  private ChartConfiguration chartConfig;
  private List<String> tableHeaders;
  private List<List<String>> tableRows;

  // -- Constructor Helpers --
  public static ReportSection createHeading(String text) {
    ReportSection s = new ReportSection();
    s.type = SectionType.HEADING;
    s.text = text;
    return s;
  }

  public static ReportSection createParagraph(String text) {
    ReportSection s = new ReportSection();
    s.type = SectionType.PARAGRAPH;
    s.text = text;
    return s;
  }

  public static ReportSection createChart(ChartConfiguration config) {
    ReportSection s = new ReportSection();
    s.type = SectionType.CHART;
    s.chartConfig = config;
    return s;
  }

  public static ReportSection createTable(List<String> headers, List<List<String>> rows) {
    ReportSection s = new ReportSection();
    s.type = SectionType.TABLE;
    s.tableHeaders = headers;
    s.tableRows = rows;
    return s;
  }

  // -- Standard Getters & Setters --
  public SectionType getType() {
    return type;
  }

  public void setType(SectionType type) {
    this.type = type;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public ChartConfiguration getChartConfig() {
    return chartConfig;
  }

  public void setChartConfig(ChartConfiguration chartConfig) {
    this.chartConfig = chartConfig;
  }

  public List<String> getTableHeaders() {
    return tableHeaders;
  }

  public void setTableHeaders(List<String> tableHeaders) {
    this.tableHeaders = tableHeaders;
  }

  public List<List<String>> getTableRows() {
    return tableRows;
  }

  public void setTableRows(List<List<String>> tableRows) {
    this.tableRows = tableRows;
  }
}