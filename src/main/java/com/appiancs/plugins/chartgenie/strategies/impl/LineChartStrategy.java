package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class LineChartStrategy implements ChartGeneratorStrategy {

  private static final Paint[] BIA_PALETTE = {
    Color.decode("#00395D"), // Barclays Dark Blue (Primary Line)
    Color.decode("#00AEEF"), // Barclays Light Blue (Secondary Line)
    Color.decode("#FF0000"), // Unsatisfactory (Red)
    Color.decode("#FFC000") // Needs Improvement (Amber)
  };

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    if (config.getMultiSeriesData() != null) {
      for (ChartDataPoint data : config.getMultiSeriesData()) {
        String series = (data.getSeries() != null) ? data.getSeries() : "Default";
        String category = (data.getCategory() != null) ? data.getCategory() : "Unknown";
        Number value = (data.getValue() != null) ? data.getValue() : 0;
        dataset.addValue(value, series, category);
      }
    }

    JFreeChart chart = ChartFactory.createLineChart(
      config.getTitle(),
      null,
      null,
      dataset);

    CategoryPlot plot = chart.getCategoryPlot();

    // Apply Barclays Colors
    plot.setDrawingSupplier(new DefaultDrawingSupplier(
      BIA_PALETTE,
      DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));

    // Formatting
    plot.setBackgroundPaint(Color.WHITE);
    chart.setBackgroundPaint(Color.WHITE);
    plot.setOutlineVisible(false);
    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

    return chart;
  }
}