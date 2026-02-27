package com.appiancs.plugins.chartgenie.dto.structure;

import java.util.List;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.TableConfiguration;

public class ReportSection {
  private String type;
  private String text;
  private String accentColor; // Fixes Compilation Error
  private ChartConfiguration chartConfig;
  private List<ReportSection> sidebarContent;
  private List<ReportSection> mainContent;
  private TableConfiguration tableConfig;
  private Double leftColumnRatio;

  public ReportSection() {
  }

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
  } // Fixes Compilation Error

  public void setAccentColor(String accentColor) {
    this.accentColor = accentColor;
  }

  public Double getLeftColumnRatio() {
    return leftColumnRatio;
  }

  public void setLeftColumnRatio(Double leftColumnRatio) {
    this.leftColumnRatio = leftColumnRatio;
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

  public TableConfiguration getTableConfig() {
    return tableConfig;
  }

  public void setTableConfig(TableConfiguration tableConfig) {
    this.tableConfig = tableConfig;
  }

  private String title;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}