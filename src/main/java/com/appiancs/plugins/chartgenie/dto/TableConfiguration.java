package com.appiancs.plugins.chartgenie.dto;

import java.util.List;

/**
 * Configuration for a styled Word table rendered by {@link com.appiancs.plugins.chartgenie.service.TableGenerator}.
 * <p>
 * Defines headers, data rows, column widths, colors, borders, and font sizes.
 * Font sizes are clamped to the range 8–72pt. Rows contain lists of {@link TableCellConfig}
 * objects which may themselves contain nested tables.
 * <p>
 * Conditional formatting rules ({@code conditionalFormats}) are evaluated in order against each
 * data row. The first matching rule wins. Each rule targets a specific column index and applies
 * a background and/or text color when its condition is met.
 */
public class TableConfiguration {
  private List<String> headers;
  private List<List<TableCellConfig>> rows;
  private List<Integer> columnWidths;
  private String headerBackgroundColor;
  private String headerTextColor;
  private String oddRowColor;
  private boolean bordersEnabled = true;
  private Integer headerFontSize;
  private Integer bodyFontSize;
  private List<ConditionalFormat> conditionalFormats;

  /**
   * A single conditional formatting rule.
   * Applied to every data row: if the value in {@code columnIndex} satisfies
   * {@code operator} against {@code value}, the row receives the specified colors.
   * <p>
   * Supported operators: {@code >}, {@code <}, {@code >=}, {@code <=}, {@code =}, {@code !=},
   * {@code contains}, {@code startsWith}, {@code endsWith}.
   */
  public static class ConditionalFormat {
    private int columnIndex;
    private String operator;
    private String value;
    private String backgroundColor;
    private String textColor;

    public int getColumnIndex() {
      return columnIndex;
    }

    public void setColumnIndex(int columnIndex) {
      this.columnIndex = columnIndex;
    }

    public String getOperator() {
      return operator;
    }

    public void setOperator(String operator) {
      this.operator = operator;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getBackgroundColor() {
      return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
      this.backgroundColor = backgroundColor;
    }

    public String getTextColor() {
      return textColor;
    }

    public void setTextColor(String textColor) {
      this.textColor = textColor;
    }
  }

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

  public boolean isBordersEnabled() {
    return bordersEnabled;
  }

  public void setBordersEnabled(boolean bordersEnabled) {
    this.bordersEnabled = bordersEnabled;
  }

  public Integer getHeaderFontSize() {
    return headerFontSize;
  }

  public void setHeaderFontSize(Integer headerFontSize) {
    this.headerFontSize = headerFontSize;
  }

  public Integer getBodyFontSize() {
    return bodyFontSize;
  }

  public void setBodyFontSize(Integer bodyFontSize) {
    this.bodyFontSize = bodyFontSize;
  }

  public List<ConditionalFormat> getConditionalFormats() {
    return conditionalFormats;
  }

  public void setConditionalFormats(List<ConditionalFormat> conditionalFormats) {
    this.conditionalFormats = conditionalFormats;
  }
}