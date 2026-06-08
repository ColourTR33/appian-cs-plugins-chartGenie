package com.appiancs.plugins.chartgenie;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.jfree.chart.JFreeChart;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.service.ChartStrategyFactory;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

class ScatterPlotUnitTest {

  private static final Logger LOG = LoggerFactory.getLogger(ScatterPlotUnitTest.class);

  @Test
  void testScatterPlotGeneration() {
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

    // Test strategy factory returns a valid strategy
    ChartGeneratorStrategy strategy = new ChartStrategyFactory().getStrategy("SCATTER");
    assertNotNull(strategy, "Scatter plot strategy should not be null");
    LOG.info("Strategy class: {}", strategy.getClass().getSimpleName());

    // Generate chart and verify
    JFreeChart chart = strategy.generate(config);
    assertNotNull(chart, "Generated scatter plot chart should not be null");
    assertNotNull(chart.getTitle(), "Chart should have a title");
    assertTrue(chart.getXYPlot().getDataset().getSeriesCount() > 0,
        "Chart dataset should contain at least one series");

    LOG.info("Scatter plot generated successfully with {} series",
        chart.getXYPlot().getDataset().getSeriesCount());
  }
}
