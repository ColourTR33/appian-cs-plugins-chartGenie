package com.appiancs.plugins.chartgenie.dto.structure;

public class DocumentSettings {
  private String templateName;
  private String outputFileName;

  // --- NEW FIELDS ---
  private String headerText;
  private String footerText;

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

  public String getHeaderText() {
    return headerText;
  }

  public void setHeaderText(String headerText) {
    this.headerText = headerText;
  }

  public String getFooterText() {
    return footerText;
  }

  public void setFooterText(String footerText) {
    this.footerText = footerText;
  }
}