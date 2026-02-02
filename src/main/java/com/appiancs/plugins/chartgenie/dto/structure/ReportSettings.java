package com.appiancs.plugins.chartgenie.dto.structure;

public class ReportSettings {
  private Boolean qrCodeEnabled;
  private String qrUrl;
  private String headerText;
  private String headerFont;
  private String headerColor;
  private String footerText;

  public Boolean getQrCodeEnabled() {
    return qrCodeEnabled;
  }

  public void setQrCodeEnabled(Boolean qrCodeEnabled) {
    this.qrCodeEnabled = qrCodeEnabled;
  }

  public String getQrUrl() {
    return qrUrl;
  }

  public void setQrUrl(String qrUrl) {
    this.qrUrl = qrUrl;
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

  public String getHeaderColor() {
    return headerColor;
  }

  public void setHeaderColor(String headerColor) {
    this.headerColor = headerColor;
  }

  public String getHeaderFont() {
    return headerFont;
  }

  public void setHeaderFont(String headerFont) {
    this.headerFont = headerFont;
  }
}