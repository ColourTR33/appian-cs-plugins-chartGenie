package com.appiancs.plugins.chartgenie.dto;

public class ChartDataPoint {
  private String series; // e.g., "Critical" (The Legend Item)
  private String category; // e.g., "BIA" (The X-Axis Label)
  private Number value; // e.g., 10 (The Y-Axis Height)

  public ChartDataPoint() {
  }

  public String getSeries() {
    return series;
  }

  public void setSeries(String series) {
    this.series = series;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Number getValue() {
    return value;
  }

  public void setValue(Number value) {
    this.value = value;
  }
}