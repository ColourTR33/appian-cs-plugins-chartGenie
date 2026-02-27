package com.appiancs.plugins.chartgenie.dto;

import java.util.List;

public class TableConfiguration {
  private List<String> headers;
  private List<List<TableCellConfig>> rows;
  private List<Integer> columnWidths;
  private String headerBackgroundColor;
  private String headerTextColor; // Fixes Compilation Error
  private String oddRowColor;
  private boolean bordersEnabled = true;

  public TableConfiguration() {
  }

  public List<String> getHeaders() {
    return headers;
  }

  public void setHeaders(List<String> headers) {
    this.headers = headers;
  }

  public List<List<TableCellConfig>> getRows() {
    return rows;
  }

  public void setRows(List<List<TableCellConfig>> rows) {
    this.rows = rows;
  }

  public List<Integer> getColumnWidths() {
    return columnWidths;
  }

  public void setColumnWidths(List<Integer> columnWidths) {
    this.columnWidths = columnWidths;
  }

  public String getHeaderBackgroundColor() {
    return headerBackgroundColor;
  }

  public void setHeaderBackgroundColor(String headerBackgroundColor) {
    this.headerBackgroundColor = headerBackgroundColor;
  }

  public String getHeaderTextColor() {
    return headerTextColor;
  } // Fixes Compilation Error

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