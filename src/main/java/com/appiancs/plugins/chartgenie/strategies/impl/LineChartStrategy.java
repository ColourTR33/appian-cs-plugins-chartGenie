package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.Color;
import java.awt.Paint;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.service.MonochromaticPaletteGenerator;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class LineChartStrategy implements ChartGeneratorStrategy {

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

    Paint[] palette = MonochromaticPaletteGenerator.resolve(config.getPrimaryColor());
    plot.setDrawingSupplier(new DefaultDrawingSupplier(
      palette,
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