package com.appiancs.plugins.chartgenie;

import java.util.Arrays;

import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.service.ChartStrategyFactory;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class ScatterPlotUnitTest {

  private static final Logger LOG = LoggerFactory.getLogger(ScatterPlotUnitTest.class);

  private static String sanitizeForLogging(String input) {
    if (input == null)
      return "null";
    String value = input.length() > 200 ? input.substring(0, 200) + "[TRUNCATED]" : input;
    return value.replaceAll("[\\r\\n\\t]", "_").replaceAll("[\\p{Cntrl}]", "").trim();
  }

  public static void main(String[] args) {
    try {
      LOG.info("--- Testing Scatter Plot Strategy ---");

      // Create test configuration
      ChartConfiguration config = new ChartConfiguration();
      config.setChartType("SCATTER");
      config.setTitle("Test Scatter Plot");

      // Create test data points
      ChartDataPoint point1 = new ChartDataPoint();
      point1.setSeries("Series A");
      point1.setCategory("1");
      point1.setValue(10);

      ChartDataPoint point2 = new ChartDataPoint();
      point2.setSeries("Series A");
      point2.setCategory("2");
      point2.setValue(20);

      ChartDataPoint point3 = new ChartDataPoint();
      point3.setSeries("Series B");
      point3.setCategory("1");
      point3.setValue(15);

      ChartDataPoint point4 = new ChartDataPoint();
      point4.setSeries("Series B");
      point4.setCategory("2");
      point4.setValue(25);

      config.setMultiSeriesData(Arrays.asList(point1, point2, point3, point4));

      // Test strategy factory
      ChartGeneratorStrategy strategy = ChartStrategyFactory.getStrategy("SCATTER");
      LOG.info("Strategy class: {}", sanitizeForLogging(strategy.getClass().getSimpleName()));

      // Generate chart
      JFreeChart chart = strategy.generate(config);
      LOG.info("Chart generated successfully!");
      String chartTitle = chart.getTitle() != null ? chart.getTitle().getText() : "No title";
      LOG.info("Chart title: {}", sanitizeForLogging(chartTitle));
      LOG.info("Dataset series count: {}", chart.getXYPlot().getDataset().getSeriesCount());

      LOG.info("--- Scatter Plot Strategy Test Completed Successfully ---");

    } catch (Exception e) {
      LOG.error("Error during scatter plot strategy test", e);
    }
  }
}