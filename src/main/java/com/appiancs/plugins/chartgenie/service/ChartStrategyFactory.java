package com.appiancs.plugins.chartgenie.service;

import java.util.Locale;

import org.apache.log4j.Logger;

import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.AreaChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.BarChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.ColumnChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.DonutChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.LineChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.PieChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.StackedColumnStrategy;

/**
 * Factory class to instantiate the correct Chart Generation Strategy.
 * Centralizes the logic for mapping user-friendly names ("PIE") to Java Classes.
 */
public class ChartStrategyFactory {

  private static final Logger LOG = Logger.getLogger(ChartStrategyFactory.class);

  // Supported Types
  private static final String TYPE_BAR = "BAR";
  private static final String TYPE_COLUMN = "COLUMN";
  private static final String TYPE_PIE = "PIE";
  private static final String TYPE_DONUT = "DONUT";
  private static final String TYPE_LINE = "LINE";
  private static final String TYPE_AREA = "AREA";
  private static final String TYPE_STACKED = "STACKED";

  /**
   * Private constructor to hide the implicit public one.
   * PMD Requirement: Utility classes should not have a public constructor.
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

    // Use Locale.ROOT to avoid locale-specific casing issues (e.g. Turkish "I")
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
        return new ColumnChartStrategy(); // Often distinct from Bar (Horizontal vs Vertical)
      case TYPE_LINE:
        return new LineChartStrategy();
      case TYPE_AREA:
        return new AreaChartStrategy();
      default:
        LOG.warn("Unknown chart type requested: '" + chartType + "'. Defaulting to BAR chart.");
        return new BarChartStrategy();
    }
  }
}