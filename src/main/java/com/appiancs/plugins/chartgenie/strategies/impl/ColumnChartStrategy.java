package com.appiancs.plugins.chartgenie.strategies.impl;

// 1. Import your DTO
import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class ColumnChartStrategy implements ChartGeneratorStrategy {

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    // Assume single series for simple column charts
    String seriesName = "Series 1";

    // This lambda works if config.getDataPoints() returns Map<String, Double>
    if (config.getDataPoints() != null) {
      config.getDataPoints().forEach((k, v) -> dataset.addValue(v, seriesName, k));
    }

    JFreeChart chart = ChartFactory.createBarChart(
      config.getTitle(),
      "Category",
      "Value",
      dataset,
      PlotOrientation.VERTICAL, // Vertical = Column Chart
      false, // Legend
      true, // Tooltips
      false // URLs
    );
    StandardChartTheme.createJFreeTheme().apply(chart);

    CategoryPlot plot = chart.getCategoryPlot();

    // Use standard Java AWT Colors
    plot.getRenderer().setSeriesPaint(0, config.getPrimaryColor());
    plot.setBackgroundPaint(Color.WHITE);
    plot.setRangeGridlinePaint(Color.GRAY);

    return chart;
  }
}