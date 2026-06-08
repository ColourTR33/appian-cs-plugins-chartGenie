package com.appiancs.plugins.chartgenie;

import java.util.Arrays;

import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.service.ChartStrategyFactory;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class AllChartTypesTest {
  private static final Logger LOG = LoggerFactory.getLogger(AllChartTypesTest.class);

  /**
   * Sanitizes input for logging to prevent CWE-398 poor logging practices.
   * 
   * @param input
   *          The input string to sanitize
   * @return Sanitized string safe for logging
   */
  private static String sanitizeForLogging(String input) {
    if (input == null) {
      return "null";
    }

    // Limit input length to prevent log flooding
    if (input.length() > 200) {
      input = input.substring(0, 200) + "[TRUNCATED]";
    }

    // Remove control characters and potential injection vectors
    return input.replaceAll("[\\r\\n\\t]", "_")
      .replaceAll("[\\p{Cntrl}]", "")
      .replaceAll("[\\x00-\\x1F\\x7F]", "")
      .trim();
  }

  public static void main(String[] args) {
    try {
      LOG.info("--- Testing All Chart Types After Security Fixes ---");

      String[] chartTypes = { "BAR", "COLUMN", "PIE", "DONUT", "LINE", "AREA", "STACKED", "SCATTER" };

      for (String chartType : chartTypes) {
        LOG.info("Testing {} chart...", sanitizeForLogging(chartType));

        // Create test configuration
        ChartConfiguration config = new ChartConfiguration();
        config.setChartType(chartType);
        config.setTitle("Test " + sanitizeForLogging(chartType) + " Chart");

        // Create test data points
        ChartDataPoint point1 = new ChartDataPoint();
        point1.setSeries("Series A");
        point1.setCategory("Cat1");
        point1.setValue(10);

        ChartDataPoint point2 = new ChartDataPoint();
        point2.setSeries("Series A");
        point2.setCategory("Cat2");
        point2.setValue(20);

        ChartDataPoint point3 = new ChartDataPoint();
        point3.setSeries("Series B");
        point3.setCategory("Cat1");
        point3.setValue(15);

        ChartDataPoint point4 = new ChartDataPoint();
        point4.setSeries("Series B");
        point4.setCategory("Cat2");
        point4.setValue(25);

        config.setMultiSeriesData(Arrays.asList(point1, point2, point3, point4));

        // Test strategy factory
        ChartGeneratorStrategy strategy = ChartStrategyFactory.getStrategy(chartType);
        LOG.info("  Strategy: {}", sanitizeForLogging(strategy.getClass().getSimpleName()));

        // Generate chart
        JFreeChart chart = strategy.generate(config);
        LOG.info("  ✓ {} chart generated successfully!", sanitizeForLogging(chartType));
      }

      LOG.info("--- All Chart Types Test Completed Successfully ---");

    } catch (Exception e) {
      LOG.error("Error during chart types test", e);
    }
  }
}