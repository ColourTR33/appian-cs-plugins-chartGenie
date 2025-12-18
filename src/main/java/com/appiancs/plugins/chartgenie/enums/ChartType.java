package com.appiancs.plugins.chartgenie.enums;

public enum ChartType {
    // --- PIE & DONUT FAMILY ---
    PIE("Pie Chart", ChartFamily.PIE),
    PIE_3D("3D Pie Chart", ChartFamily.PIE),
    DONUT("Donut Chart", ChartFamily.PIE), // Technically a Ring Chart

    // --- BAR & COLUMN FAMILY (Category Data) ---
    // 'Column' usually implies Vertical, 'Bar' implies Horizontal
    COLUMN("Column Chart", ChartFamily.CATEGORY),
    COLUMN_3D("3D Column Chart", ChartFamily.CATEGORY),
    COLUMN_STACKED("Stacked Column Chart", ChartFamily.CATEGORY),

    BAR("Bar Chart", ChartFamily.CATEGORY),
    BAR_3D("3D Bar Chart", ChartFamily.CATEGORY),
    BAR_STACKED("Stacked Bar Chart", ChartFamily.CATEGORY),

    // --- LINE & AREA FAMILY (Category Data) ---
    LINE("Line Chart", ChartFamily.CATEGORY),
    LINE_3D("3D Line Chart", ChartFamily.CATEGORY),
    AREA("Area Chart", ChartFamily.CATEGORY),
    AREA_STACKED("Stacked Area Chart", ChartFamily.CATEGORY),

    // --- XY PLOTS (Numeric X and Y axes) ---
    SCATTER("Scatter Plot", ChartFamily.XY),
    BUBBLE("Bubble Chart", ChartFamily.XY),
    XY_LINE("XY Line Chart", ChartFamily.XY),
    XY_AREA("XY Area Chart", ChartFamily.XY),

    // --- SPECIALIZED ---
    TIME_SERIES("Time Series Chart", ChartFamily.XY), // Treats X axis as Dates
    GANTT("Gantt Chart", ChartFamily.INTERVAL), // Requires IntervalCategoryDataset
    CANDLESTICK("Candlestick Chart", ChartFamily.OHLC), // Requires OHLCDataset
    RADAR("Radar (Spider) Chart", ChartFamily.CATEGORY);

  private final String friendlyName;
  private final ChartFamily family;

  ChartType(String friendlyName, ChartFamily family) {
    this.friendlyName = friendlyName;
    this.family = family;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public ChartFamily getFamily() {
    return family;
  }

  // Helper to find enum safely from Appian string input
  public static ChartType fromString(String text) {
    for (ChartType b : ChartType.values()) {
      if (b.name().equalsIgnoreCase(text)) {
        return b;
      }
    }
    return COLUMN; // Default fallback
  }
}
