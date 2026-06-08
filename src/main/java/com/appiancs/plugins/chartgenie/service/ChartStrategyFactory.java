package com.appiancs.plugins.chartgenie.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.AreaChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.BarChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.ColumnChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.DonutChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.LineChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.PieChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.ScatterPlotStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.StackedColumnStrategy;

/**
 * Factory class to instantiate the correct Chart Generation Strategy.
 * Centralises the logic for mapping user-friendly names ("PIE") to Java Classes.
 */
public class ChartStrategyFactory {

  // FIXED: Correct SLF4J initialization
  private static final Logger LOG = LoggerFactory.getLogger(ChartStrategyFactory.class);

  // Supported Types
  private static final String TYPE_BAR = "BAR";
  private static final String TYPE_COLUMN = "COLUMN";
  private static final String TYPE_PIE = "PIE";
  private static final String TYPE_DONUT = "DONUT";
  private static final String TYPE_LINE = "LINE";
  private static final String TYPE_AREA = "AREA";
  private static final String TYPE_STACKED = "STACKED";
  private static final String TYPE_SCATTER = "SCATTER";

  /**
   * Private constructor to hide the implicit public one.
   */
  private ChartStrategyFactory() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Returns a strategy instance for the requested chart type.
   *
   * @param chartType
   *          The name of the chart (e.g., "PIE", "BAR").
   * @return A concrete ChartGeneratorStrategy. Defaults to BAR if unknown.
   */
  public static ChartGeneratorStrategy getStrategy(String chartType) {
    if (chartType == null) {
      LOG.warn("Chart type is null. Defaulting to BAR chart.");
      return new BarChartStrategy();
    }

    // Use Locale.ROOT to avoid locale-specific casing issues
    String normalizedType = chartType.trim().toUpperCase(Locale.ROOT);

    switch (normalizedType) {
      case TYPE_DONUT:
        return new DonutChartStrategy();
      case TYPE_PIE:
        return new PieChartStrategy();
      case TYPE_STACKED:
        return new StackedColumnStrategy();
      case TYPE_BAR:
        return new BarChartStrategy();
      case TYPE_COLUMN:
        return new ColumnChartStrategy();
      case TYPE_LINE:
        return new LineChartStrategy();
      case TYPE_AREA:
        return new AreaChartStrategy();
      case TYPE_SCATTER:
        return new ScatterPlotStrategy();
      default:
        // FIXED: Using SLF4J parameterized logging
        LOG.warn("Unknown chart type requested: '{}'. Defaulting to BAR chart.", chartType);
        return new BarChartStrategy();
    }
  }
}