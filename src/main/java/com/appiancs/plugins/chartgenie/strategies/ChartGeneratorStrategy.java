package com.appiancs.plugins.chartgenie.strategies;

import org.jfree.chart.JFreeChart;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;

/**
 * Common interface for all Chart Generation Strategies.
 * <p>
 * This interface allows the application to swap chart algorithms (Bar, Pie, Line)
 * dynamically at runtime without changing the consuming service code.
 * </p>
 */
public interface ChartGeneratorStrategy {

  /**
   * Generates a configured JFreeChart object.
   *
   * @param config
   *          The configuration object containing data, title, and styling preferences.
   * @return A fully constructed and styled JFreeChart instance.
   */
  JFreeChart generate(ChartConfiguration config);

}