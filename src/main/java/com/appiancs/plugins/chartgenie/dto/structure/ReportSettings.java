package com.appiancs.plugins.chartgenie.dto.structure;

public class ReportSettings {
  private String headerText;
  private String subheaderText;
  private String footerText;
  private String headerColor;
  private String orientation; // PORTRAIT or LANDSCAPE
  private String pageSize; // A4 or LETTER

  // NEW FIELDS FOR THE RATING
  private String headerRating;
  private boolean showRatingInHeader;

  private boolean qrCodeEnabled;
  private String qrUrl;

  // Standard Getters and Setters
  public String getHeaderText() {
    return headerText;
  }

  public void setHeaderText(String headerText) {
    this.headerText = headerText;
  }

  public String getSubheaderText() {
    return subheaderText;
  }

  public void setSubheaderText(String subheaderText) {
    this.subheaderText = subheaderText;
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

  public String getOrientation() {
    return orientation;
  }

  public void setOrientation(String orientation) {
    this.orientation = orientation;
  }

  public String getPageSize() {
    return pageSize;
  }

  public void setPageSize(String pageSize) {
    this.pageSize = pageSize;
  }

  public String getHeaderRating() {
    return headerRating;
  }

  public void setHeaderRating(String headerRating) {
    this.headerRating = headerRating;
  }

  public boolean isShowRatingInHeader() {
    return showRatingInHeader;
  }

  public void setShowRatingInHeader(boolean showRatingInHeader) {
    this.showRatingInHeader = showRatingInHeader;
  }

  public boolean isQrCodeEnabled() {
    return qrCodeEnabled;
  }

  public void setQrCodeEnabled(boolean qrCodeEnabled) {
    this.qrCodeEnabled = qrCodeEnabled;
  }

  public String getQrUrl() {
    return qrUrl;
  }

  public void setQrUrl(String qrUrl) {
    this.qrUrl = qrUrl;
  }
}