package com.appiancs.plugins.chartgenie.dto.structure;

import java.util.ArrayList;
import java.util.List;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;

public class ReportSection {

  public enum SectionType {
      HEADING,
      PARAGRAPH,
      CHART,
      TABLE,
      PAGE_BREAK,
      SIDEBAR_LAYOUT
  }

  private SectionType type;

  // Existing fields...
  private String text;
  private ChartConfiguration chartConfig;
  private List<String> tableHeaders;
  private List<List<String>> tableRows;

  // --- NEW FIELDS FOR LAYOUTS ---
  private List<ReportSection> leftContent = new ArrayList<>();
  private List<ReportSection> rightContent = new ArrayList<>();

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

  // --- HELPER CONSTRUCTOR ---
  public static ReportSection createSidebarLayout(List<ReportSection> left, List<ReportSection> right) {
    ReportSection s = new ReportSection();
    s.type = SectionType.SIDEBAR_LAYOUT;
    s.leftContent = left;
    s.rightContent = right;
    return s;
  }

  // Getters and Setters for new lists
  public List<ReportSection> getLeftContent() {
    return leftContent;
  }

  public void setLeftContent(List<ReportSection> leftContent) {
    this.leftContent = leftContent;
  }

  public List<ReportSection> getRightContent() {
    return rightContent;
  }

  public void setRightContent(List<ReportSection> rightContent) {
    this.rightContent = rightContent;
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