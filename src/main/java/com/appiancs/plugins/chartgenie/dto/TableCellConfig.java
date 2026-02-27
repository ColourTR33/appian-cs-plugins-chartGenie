package com.appiancs.plugins.chartgenie.dto;

public class TableCellConfig {
  private String text;
  private Integer colspan;
  private String backgroundColor;
  private String textColor;

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
}