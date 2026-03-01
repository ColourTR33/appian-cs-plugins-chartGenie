package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class BarChartStrategy implements ChartGeneratorStrategy {

  private static final Paint[] BIA_PALETTE = {
    Color.decode("#00AEEF"), // Barclays Light Blue
    Color.decode("#00395D"), // Barclays Dark Blue
    Color.decode("#FF0000"), // Unsatisfactory (Red)
    Color.decode("#FFC000"), // Needs Improvement (Amber)
    Color.decode("#00B050"), // Satisfactory (Green)
    Color.decode("#00B0F0") // Mature (Cyan)
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

    JFreeChart chart = ChartFactory.createBarChart(
      null,
      null, // X-Axis Label
      null, // Y-Axis Label
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

    // Clean White Background formatting
    plot.setBackgroundPaint(new Color(0, 0, 0, 0));
    chart.setBackgroundPaint(null);
    plot.setOutlineVisible(false);

    // --- THE FIX: SCALE UP THE AXIS FONTS ---
    Font largeAxisFont = new Font("SansSerif", Font.PLAIN, 30);

    CategoryAxis domainAxis = plot.getDomainAxis(); // X-Axis (Categories)
    domainAxis.setTickLabelFont(largeAxisFont);

    ValueAxis rangeAxis = plot.getRangeAxis(); // Y-Axis (Numbers)
    rangeAxis.setTickLabelFont(largeAxisFont);
    // ----------------------------------------

    LegendTitle legend = chart.getLegend();
    if (legend != null) {
      legend.setBackgroundPaint(null); // Transparent legend background
      legend.setItemFont(new Font("SansSerif", Font.BOLD, 50));
    }

    return chart;
  }
}