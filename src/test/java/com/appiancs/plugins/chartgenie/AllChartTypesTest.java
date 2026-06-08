package com.appiancs.plugins.chartgenie;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import org.jfree.chart.JFreeChart;
import org.junit.jupiter.api.Test;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.service.ChartStrategyFactory;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

/**
 * JUnit 5 tests verifying that all supported chart types generate successfully.
 */
class AllChartTypesTest {

  private final ChartStrategyFactory strategyFactory = new ChartStrategyFactory();

  private ChartConfiguration createTestConfig(String chartType) {
    ChartConfiguration config = new ChartConfiguration();
    config.setChartType(chartType);
    config.setTitle("Test " + chartType + " Chart");

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
    return config;
  }

  @Test
  void testBarChart() {
    ChartConfiguration config = createTestConfig("BAR");
    ChartGeneratorStrategy strategy = strategyFactory.getStrategy("BAR");
    JFreeChart chart = strategy.generate(config);
    assertNotNull(chart, "BAR chart should not be null");
  }

  @Test
  void testColumnChart() {
    ChartConfiguration config = createTestConfig("COLUMN");
    ChartGeneratorStrategy strategy = strategyFactory.getStrategy("COLUMN");
    JFreeChart chart = strategy.generate(config);
    assertNotNull(chart, "COLUMN chart should not be null");
  }

  @Test
  void testPieChart() {
    ChartConfiguration config = createTestConfig("PIE");
    ChartGeneratorStrategy strategy = strategyFactory.getStrategy("PIE");
    JFreeChart chart = strategy.generate(config);
    assertNotNull(chart, "PIE chart should not be null");
  }

  @Test
  void testDonutChart() {
    ChartConfiguration config = createTestConfig("DONUT");
    ChartGeneratorStrategy strategy = strategyFactory.getStrategy("DONUT");
    JFreeChart chart = strategy.generate(config);
    assertNotNull(chart, "DONUT chart should not be null");
  }

  @Test
  void testLineChart() {
    ChartConfiguration config = createTestConfig("LINE");
    ChartGeneratorStrategy strategy = strategyFactory.getStrategy("LINE");
    JFreeChart chart = strategy.generate(config);
    assertNotNull(chart, "LINE chart should not be null");
  }

  @Test
  void testAreaChart() {
    ChartConfiguration config = createTestConfig("AREA");
    ChartGeneratorStrategy strategy = strategyFactory.getStrategy("AREA");
    JFreeChart chart = strategy.generate(config);
    assertNotNull(chart, "AREA chart should not be null");
  }

  @Test
  void testStackedChart() {
    ChartConfiguration config = createTestConfig("STACKED");
    ChartGeneratorStrategy strategy = strategyFactory.getStrategy("STACKED");
    JFreeChart chart = strategy.generate(config);
    assertNotNull(chart, "STACKED chart should not be null");
  }

  @Test
  void testScatterChart() {
    ChartConfiguration config = createTestConfig("SCATTER");
    ChartGeneratorStrategy strategy = strategyFactory.getStrategy("SCATTER");
    JFreeChart chart = strategy.generate(config);
    assertNotNull(chart, "SCATTER chart should not be null");
  }
}
