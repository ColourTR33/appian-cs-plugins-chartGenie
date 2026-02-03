package com.appiancs.plugins.chartgenie.dto;

import java.util.List;

public class TableConfiguration {
  private List<String> headers;
  private List<List<String>> rows; // Data: List of Rows, where each Row is a List of Cells
  private List<Integer> columnWidths; // Optional: Percentages (e.g., [30, 70])

  // Styling
  private String headerBackgroundColor; // e.g., "2F5496"
  private String headerTextColor; // e.g., "FFFFFF"
  private String oddRowColor; // e.g., "F2F2F2" (Zebra striping)
  private boolean bordersEnabled = true;

  // ... Getters and Setters for all ...
  public List<String> getHeaders() {
    return headers;
  }

  public void setHeaders(List<String> headers) {
    this.headers = headers;
  }

  public List<List<String>> getRows() {
    return rows;
  }

  public void setRows(List<List<String>> rows) {
    this.rows = rows;
  }

  public String getHeaderBackgroundColor() {
    return headerBackgroundColor;
  }

  public void setHeaderBackgroundColor(String headerBackgroundColor) {
    this.headerBackgroundColor = headerBackgroundColor;
  }

  public String getHeaderTextColor() {
    return headerTextColor;
  }

  public void setHeaderTextColor(String headerTextColor) {
    this.headerTextColor = headerTextColor;
  }

  public String getOddRowColor() {
    return oddRowColor;
  }

  public void setOddRowColor(String oddRowColor) {
    this.oddRowColor = oddRowColor;
  }
}