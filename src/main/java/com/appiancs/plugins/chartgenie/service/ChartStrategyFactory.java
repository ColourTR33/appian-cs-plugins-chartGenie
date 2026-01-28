package com.appiancs.plugins.chartgenie.service;

import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.*;

public class ChartStrategyFactory {

  public static ChartGeneratorStrategy getStrategy(String chartType) {
    if (chartType == null) {
      return new BarChartStrategy(); // Default fallback
    }

    switch (chartType.toUpperCase()) {
      case "DONUT":
        return new DonutChartStrategy();
      case "PIE":
        return new PieChartStrategy(); // New
      case "STACKED":
        return new StackedColumnStrategy();
      case "BAR":
        return new BarChartStrategy();
      case "LINE":
        return new LineChartStrategy(); // New
      case "AREA":
        return new AreaChartStrategy(); // New
      default:
        // Log warning here if possible
        return new BarChartStrategy();
    }
  }
}