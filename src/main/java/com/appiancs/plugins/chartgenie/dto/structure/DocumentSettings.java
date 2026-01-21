package com.appiancs.plugins.chartgenie.dto.structure;

public class DocumentSettings {
  private String templateName;
  private String outputFileName;

  // Constructors
  public DocumentSettings() {
  }

  public DocumentSettings(String templateName, String outputFileName) {
    this.templateName = templateName;
    this.outputFileName = outputFileName;
  }

  // Getters & Setters
  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public String getOutputFileName() {
    return outputFileName;
  }

  public void setOutputFileName(String outputFileName) {
    this.outputFileName = outputFileName;
  }
}