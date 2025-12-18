package com.appiancs.plugins.chartgenie.dto;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import com.appiancorp.suiteapi.content.ContentService;

public class ChartConfiguration {

  // Service Dependencies
  public ContentService cs;

  // -------------------
  // INPUT PARAMETERS
  // -------------------
  /** The desired name for the new PDF document. */
  public String targetChartDocumentName;

  /** The optional description for the new PDF document. */
  public String targetChartDocumentDesc;

  /** The folder where the new PDF document will be saved. */
  public Long targetChartFolder;

  /**
   * e.g. DONUT, COLUMN etc
   **/
  private String chartType; // "DONUT", "COLUMN", "LINE"
  private String title;
  private int width;
  private int height;

  // Data
  private Map<String, Double> dataPoints;
  private List<String> hexColors; // e.g., ["#FF0000", "#0000FF"]
  private String centerText; // Specific to Donut private Color parseHex(String hex) {

  public String getChartType() {
    return chartType;
  }

  // Standard Setter
  public void setChartType(String chartType) {
    this.chartType = chartType;
  }

  public com.appiancs.plugins.chartgenie.enums.ChartType getChartTypeEnum() {
    return com.appiancs.plugins.chartgenie.enums.ChartType.fromString(this.chartType);
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    // Trimming is good practice to avoid invisible whitespace issues from Appian
    if (title != null) {
      this.title = title.trim();
    } else {
      this.title = null;
    }
  }

  public int getWidth() {
    // Double check safety in getter, though setter handles it
    return (width <= 0) ? 600 : width;
  }

  public void setWidth(int width) {
    if (width <= 0) {
      this.width = 600; // Default reasonable width
    } else {
      this.width = width;
    }
  }

  public int getHeight() {
    return (height <= 0) ? 400 : height;
  }

  public void setHeight(int height) {
    if (height <= 0) {
      this.height = 400; // Default reasonable height
    } else {
      this.height = height;
    }
  }

  public Map<String, Double> getDataPoints() {
    return dataPoints;
  }

  public void setDataPoints(Map<String, Double> dataPoints) {
    this.dataPoints = dataPoints;
  }

  /**
   * SPECIALISED HELPER: Call this from your Smart Service run() method.
   * Appian passes data as two separate lists, so we merge them here.
   * * @param categories List of Strings (X-axis/Labels)
   * 
   * @param values
   *          List of Numbers (Y-axis/Values)
   */
  public void setDataFromLists(List<String> categories, List<Number> values) {
    this.dataPoints = new java.util.LinkedHashMap<>(); // LinkedHashMap preserves insertion order!

    if (categories == null || values == null) {
      return;
    }

    // Use the smaller size to avoid IndexOutOfBoundsException
    int size = Math.min(categories.size(), values.size());

    for (int i = 0; i < size; i++) {
      String key = categories.get(i);
      Number val = values.get(i);

      // Handle potential nulls in the Appian list
      if (key != null && val != null) {
        this.dataPoints.put(key, val.doubleValue());
      }
    }
  }

  public List<String> getHexColors() {
    // Return an empty list instead of null to be safe
    if (hexColors == null) {
      return new java.util.ArrayList<>();
    }
    return hexColors;
  }

  public void setHexColors(List<String> hexColors) {
    this.hexColors = hexColors;
  }

  public Color getPrimaryColor() {
    if (hexColors != null && !hexColors.isEmpty()) {
      // formatting checks are handled in parseHex
      return parseHex(hexColors.get(0));
    }
    return Color.BLACK; // Default fallback
  }

  public Color getSecondaryColor() {
    if (hexColors != null && hexColors.size() > 1) {
      return parseHex(hexColors.get(1));
    }
    return Color.LIGHT_GRAY; // Default fallback
  }

  public String getCenterText() {
    return centerText;
  }

  public void setCenterText(String centerText) {
    if (centerText != null) {
      this.centerText = centerText.trim();
    } else {
      this.centerText = null;
    }
  }

  private Color parseHex(String hex) {
    if (hex == null || hex.trim().isEmpty()) {
      return Color.BLACK;
    }
    try {
      // Color.decode handles "#FFFFFF" format perfectly
      return Color.decode(hex.trim());
    } catch (NumberFormatException e) {
      // SAFETY: If user sends bad data, do not crash the plugin.
      // Return a default color and maybe log it.
      System.err.println("Invalid Hex Color received: " + hex);
      return Color.BLACK;
    }
  }

  // Output Field
  public Long newChartDocumentCreated;
}
