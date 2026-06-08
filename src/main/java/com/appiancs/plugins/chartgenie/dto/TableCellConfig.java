package com.appiancs.plugins.chartgenie.dto;

/**
 * Configuration for a single table cell within a {@link TableConfiguration} row.
 * <p>
 * Supports plain text, HTML rich text, column spanning, background/text color overrides,
 * and optionally a fully nested {@link TableConfiguration} rendered inside the cell.
 * Nested tables are supported up to 3 levels deep.
 */
public class TableCellConfig {
  private String text;
  private Integer colspan;
  private String backgroundColor;
  private String textColor;
  private TableConfiguration nestedTable;

  public TableCellConfig() {
  }

  // Getters and Setters
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Integer getColspan() {
    return colspan;
  }

  public void setColspan(Integer colspan) {
    this.colspan = colspan;
  }

  public String getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(String bg) {
    this.backgroundColor = bg;
  }

  public String getTextColor() {
    return textColor;
  }

  public void setTextColor(String tc) {
    this.textColor = tc;
  }

  public TableConfiguration getNestedTable() {
    return nestedTable;
  }

  public void setNestedTable(TableConfiguration nestedTable) {
    this.nestedTable = nestedTable;
  }
}