package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

/**
 * Strategy to generate an Area Chart.
 */
public class AreaChartStrategy implements ChartGeneratorStrategy {

  // Defaults
  private static final Color COLOR_BG_DEFAULT = new Color(255, 255, 255);
  private static final Color COLOR_GRIDLINES = new Color(220, 220, 220);
  private static final Color COLOR_PRIMARY_DEFAULT = new Color(0, 184, 212);

  private static final String DEFAULT_FONT = "SansSerif";
  private static final String DEFAULT_SERIES_NAME = "Data";

  // Layout
  private static final int FONT_SIZE_TITLE = 18;
  private static final int FONT_SIZE_AXIS = 10;
  private static final int ALPHA_TRANSPARENCY = 180;

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
        dataset.addValue(values.get(i), seriesName, categories.get(i));
      }
    }

    // 2. Create Chart
    JFreeChart chart = ChartFactory.createAreaChart(
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
    domainAxis.setLowerMargin(0.0);
    domainAxis.setUpperMargin(0.0);

    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setTickLabelFont(new Font(fontName, Font.PLAIN, FONT_SIZE_AXIS));
    rangeAxis.setAutoRangeIncludesZero(true);

    // 5. Renderer
    Color primaryColor = decodeColor(config.getPrimaryColor(), COLOR_PRIMARY_DEFAULT);
    Color transparentColor = new Color(primaryColor.getRed(),
      primaryColor.getGreen(),
      primaryColor.getBlue(),
      ALPHA_TRANSPARENCY);

    AreaRenderer renderer = (AreaRenderer) plot.getRenderer();
    renderer.setSeriesPaint(0, transparentColor);

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