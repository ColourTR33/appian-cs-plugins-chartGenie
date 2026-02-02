package com.appiancs.plugins.chartgenie.dto;

import java.util.List;

public class ChartConfiguration {
  // Core Data
  private String chartType;
  private String title;
  private List<String> categories;
  private List<Number> values;

  // Dimensions (Integer for null checks)
  private Integer width;
  private Integer height;

  // Styling & Options
  private String primaryColor;
  private String seriesName;
  private String fontFamily;
  private String orientation;
  private Boolean dataLabelsEnabled;

  // NEW FIELD (Required for Service Class)
  private String metrics;

  // --- Getters and Setters ---

  public String getChartType() {
    return chartType;
  }

  public void setChartType(String chartType) {
    this.chartType = chartType;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<String> getCategories() {
    return categories;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
  }

  public List<Number> getValues() {
    return values;
  }

  public void setValues(List<Number> values) {
    this.values = values;
  }

  public Integer getWidth() {
    return width;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  public String getPrimaryColor() {
    return primaryColor;
  }

  public void setPrimaryColor(String primaryColor) {
    this.primaryColor = primaryColor;
  }

  public String getSeriesName() {
    return seriesName;
  }

  public void setSeriesName(String seriesName) {
    this.seriesName = seriesName;
  }

  public String getFontFamily() {
    return fontFamily;
  }

  public void setFontFamily(String fontFamily) {
    this.fontFamily = fontFamily;
  }

  public String getOrientation() {
    return orientation;
  }

  public void setOrientation(String orientation) {
    this.orientation = orientation;
  }

  public Boolean getDataLabelsEnabled() {
    return dataLabelsEnabled;
  }

  public void setDataLabelsEnabled(Boolean dataLabelsEnabled) {
    this.dataLabelsEnabled = dataLabelsEnabled;
  }

  public String getMetrics() {
    return metrics;
  }

  public void setMetrics(String metrics) {
    this.metrics = metrics;
  }
}