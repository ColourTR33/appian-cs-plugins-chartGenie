package com.appiancs.plugins.chartgenie.dto.structure;

import java.util.List;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.TableConfiguration;

public class ReportSection {
  private String type;
  private String text;

  // REFACTOR: Renamed from 'color' to 'accentColor' for clarity.
  // Usage: Background color for Badges, or Text color for specific highlights.
  private String accentColor;

  private ChartConfiguration chartConfig;
  private List<ReportSection> sidebarContent;
  private List<ReportSection> mainContent;

  private TableConfiguration tableConfig;

  public TableConfiguration getTableConfig() {
    return tableConfig;
  }

  public void setTableConfig(TableConfiguration tableConfig) {
    this.tableConfig = tableConfig;
  }

  public ReportSection() {
  }

  // --- Getters & Setters ---

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getAccentColor() {
    return accentColor;
  }

  public void setAccentColor(String accentColor) {
    this.accentColor = accentColor;
  }

  public ChartConfiguration getChartConfig() {
    return chartConfig;
  }

  public void setChartConfig(ChartConfiguration chartConfig) {
    this.chartConfig = chartConfig;
  }

  public List<ReportSection> getSidebarContent() {
    return sidebarContent;
  }

  public void setSidebarContent(List<ReportSection> sidebarContent) {
    this.sidebarContent = sidebarContent;
  }

  public List<ReportSection> getMainContent() {
    return mainContent;
  }

  public void setMainContent(List<ReportSection> mainContent) {
    this.mainContent = mainContent;
  }
}