package com.appiancs.plugins.chartgenie.dto.structure;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class DocumentSettings implements Serializable {

  private static final long serialVersionUID = 1L;

  @SerializedName("outputFileName")
  private String outputFileName;

  @SerializedName("primaryColor")
  private String primaryColor;

  @SerializedName("fontFamily")
  private String fontFamily;

  @SerializedName("pageSize")
  private String pageSize; // "A4" or "LETTER"

  // --- FUTURE STUBS ---
  @SerializedName("qrCodeEnabled")
  private boolean qrCodeEnabled;

  @SerializedName("qrUrl")
  private String qrUrl;

  public DocumentSettings() {
    // Defaults
    this.primaryColor = "000000";
    this.fontFamily = "SansSerif";
    this.pageSize = "A4";
    this.qrCodeEnabled = false;
  }

  // --- Getters & Setters ---

  public String getOutputFileName() {
    return outputFileName;
  }

  public void setOutputFileName(String outputFileName) {
    this.outputFileName = outputFileName;
  }

  public String getPrimaryColor() {
    return primaryColor;
  }

  public void setPrimaryColor(String primaryColor) {
    this.primaryColor = primaryColor;
  }

  public String getFontFamily() {
    return fontFamily;
  }

  public void setFontFamily(String fontFamily) {
    this.fontFamily = fontFamily;
  }

  public String getPageSize() {
    return pageSize;
  }

  public void setPageSize(String pageSize) {
    this.pageSize = pageSize;
  }

  // --- Stub Getters/Setters ---

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