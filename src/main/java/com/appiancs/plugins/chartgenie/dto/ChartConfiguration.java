package com.appiancs.plugins.chartgenie.dto;

import java.util.List;

public class ChartConfiguration {
  // Core Data (Legacy / Single Series)
  private String chartType;
  private String title;
  private List<String> categories;
  private List<Number> values;

  private String centerText;

  // ENTERPRISE FIX: Multi-Series Data (Required for Grouped/Stacked Audit Charts)
  private List<ChartDataPoint> multiSeriesData;

  // Dimensions (Integer for null checks)
  private Integer width;
  private Integer height;

  // Styling & Options
  private String primaryColor;
  private String seriesName;
  private String fontFamily;
  private String orientation;
  private Boolean dataLabelsEnabled;

  // ENTERPRISE FIX: Visual Customizations
  private String backgroundColor; // e.g., "F2F2F2"
  private String legendPosition; // "BOTTOM", "RIGHT", "TOP", "LEFT", "NONE"

  // Add this field and its getter/setter
  private Integer baseFontSize; // e.g., 24 (Default to something large if scaled down)

  public Integer getBaseFontSize() {
    return baseFontSize;
  }

  public void setBaseFontSize(Integer baseFontSize) {
    this.baseFontSize = baseFontSize;
  }

  // Required for Service Class
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

  public List<ChartDataPoint> getMultiSeriesData() {
    return multiSeriesData;
  }

  public void setMultiSeriesData(List<ChartDataPoint> multiSeriesData) {
    this.multiSeriesData = multiSeriesData;
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

  public String getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(String backgroundColor) {
    this.backgroundColor = backgroundColor;
  }

  public String getLegendPosition() {
    return legendPosition;
  }

  public void setLegendPosition(String legendPosition) {
    this.legendPosition = legendPosition;
  }

  public String getMetrics() {
    return metrics;
  }

  public void setMetrics(String metrics) {
    this.metrics = metrics;
  }

  public String getCenterText() {
    return centerText;
  }

  public void setCenterText(String centerText) {
    this.centerText = centerText;
  }
}