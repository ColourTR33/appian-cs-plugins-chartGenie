package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

/**
 * Strategy to generate a Line Chart (Trend).
 */
public class LineChartStrategy implements ChartGeneratorStrategy {

  // Defaults
  private static final Color COLOR_BG_DEFAULT = Color.WHITE;
  private static final Color COLOR_PRIMARY_DEFAULT = new Color(30, 60, 150);
  private static final Color COLOR_GRIDLINES = new Color(220, 220, 220);

  private static final String DEFAULT_FONT = "SansSerif";
  private static final String DEFAULT_SERIES_NAME = "Trend";

  // Layout
  private static final int FONT_SIZE_TITLE = 18;
  private static final int FONT_SIZE_AXIS = 10;
  private static final float LINE_THICKNESS = 3.0f;

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    // 1. Prepare Dataset
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    List<String> categories = config.getCategories();
    List<Number> values = config.getValues();

    String seriesName = (config.getSeriesName() != null && !config.getSeriesName().isEmpty())
      ? config.getSeriesName()
      : DEFAULT_SERIES_NAME;

    if (categories != null && values != null) {
      int size = Math.min(categories.size(), values.size());
      for (int i = 0; i < size; i++) {
        Number val = values.get(i);
        if (val != null) {
          dataset.addValue(val, seriesName, categories.get(i));
        }
      }
    }

    // 2. Create Chart
    JFreeChart chart = ChartFactory.createLineChart(
      config.getTitle(),
      "",
      "",
      dataset,
      PlotOrientation.VERTICAL,
      false,
      true,
      false);

    // 3. Global Styling
    chart.setBackgroundPaint(COLOR_BG_DEFAULT);
    chart.setBorderVisible(false);

    // Resolve Font
    String fontName = (config.getFontFamily() != null) ? config.getFontFamily() : DEFAULT_FONT;

    // Title
    chart.getTitle().setFont(new Font(fontName, Font.BOLD, FONT_SIZE_TITLE));
    chart.getTitle().setBackgroundPaint(COLOR_BG_DEFAULT);

    // 4. Plot Styling
    CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(COLOR_BG_DEFAULT);
    plot.setOutlineVisible(false);
    plot.setRangeGridlinePaint(COLOR_GRIDLINES);
    plot.setDomainGridlinesVisible(false);

    // Axis
    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setTickLabelFont(new Font(fontName, Font.PLAIN, FONT_SIZE_AXIS));
    domainAxis.setLowerMargin(0.02);
    domainAxis.setUpperMargin(0.02);

    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setTickLabelFont(new Font(fontName, Font.PLAIN, FONT_SIZE_AXIS));
    rangeAxis.setAutoRangeIncludesZero(true);

    // 5. Renderer
    Color primaryColor = decodeColor(config.getPrimaryColor(), COLOR_PRIMARY_DEFAULT);

    LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
    renderer.setSeriesPaint(0, primaryColor);
    renderer.setSeriesStroke(0, new BasicStroke(LINE_THICKNESS));
    renderer.setSeriesShapesVisible(0, true);
    renderer.setDrawOutlines(true);
    renderer.setUseFillPaint(true);
    renderer.setSeriesFillPaint(0, Color.WHITE);

    return chart;
  }

  private Color decodeColor(String hexStr, Color fallback) {
    if (hexStr == null || hexStr.isEmpty()) {
      return fallback;
    }
    try {
      String cleanHex = hexStr.startsWith("#") ? hexStr : "#" + hexStr;
      return Color.decode(cleanHex);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}