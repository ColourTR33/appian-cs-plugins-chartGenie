package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class StackedColumnStrategy implements ChartGeneratorStrategy {

  private static final Color COLOR_PRIMARY_DEFAULT = new Color(30, 60, 150);
  private static final Color COLOR_GRIDLINES = new Color(220, 220, 220);
  private static final String DEFAULT_FONT = "SansSerif";

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    if (config.getMultiSeriesData() != null && !config.getMultiSeriesData().isEmpty()) {
      for (ChartDataPoint point : config.getMultiSeriesData()) {
        dataset.addValue(point.getValue(), point.getSeries(), point.getCategory());
      }
    } else if (config.getCategories() != null && config.getValues() != null) {
      String seriesName = (config.getSeriesName() != null && !config.getSeriesName().isEmpty()) ? config.getSeriesName() : "Series 1";
      int size = Math.min(config.getCategories().size(), config.getValues().size());
      for (int i = 0; i < size; i++) {
        Number val = config.getValues().get(i);
        dataset.addValue(val != null ? val.doubleValue() : 0.0, seriesName, config.getCategories().get(i));
      }
    }

    JFreeChart chart = ChartFactory.createStackedBarChart(config.getTitle(), "", "", dataset, PlotOrientation.VERTICAL, true, false, false);

    Color currentBgColor = Color.WHITE;
    if (config.getBackgroundColor() != null && !config.getBackgroundColor().isEmpty()) {
      try {
        currentBgColor = Color.decode("#" + config.getBackgroundColor().replace("#", ""));
      } catch (NumberFormatException ignored) {
      }
    }
    chart.setBackgroundPaint(currentBgColor);
    chart.setBorderVisible(false);

    int fontSize = (config.getBaseFontSize() != null && config.getBaseFontSize() > 0) ? config.getBaseFontSize() : 18;
    String fontName = (config.getFontFamily() != null) ? config.getFontFamily() : DEFAULT_FONT;

    if (chart.getTitle() != null) {
      chart.getTitle().setFont(new Font(fontName, Font.BOLD, fontSize + 6));
      chart.getTitle().setBackgroundPaint(currentBgColor);
    }

    CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(currentBgColor);
    plot.setOutlineVisible(false);
    plot.setRangeGridlinePaint(COLOR_GRIDLINES);
    plot.setDomainGridlinesVisible(false);

    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setTickLabelFont(new Font(fontName, Font.PLAIN, fontSize));
    domainAxis.setLabelFont(new Font(fontName, Font.BOLD, fontSize + 2));

    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setTickLabelFont(new Font(fontName, Font.PLAIN, fontSize));
    rangeAxis.setLabelFont(new Font(fontName, Font.BOLD, fontSize + 2));

    StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
    renderer.setBarPainter(new StandardBarPainter());
    renderer.setShadowVisible(false);
    renderer.setDrawBarOutline(false);
    renderer.setSeriesPaint(0, decodeColor(config.getPrimaryColor(), COLOR_PRIMARY_DEFAULT));

    if ("NONE".equalsIgnoreCase(config.getLegendPosition())) {
      chart.removeLegend();
    } else if (chart.getLegend() != null) {
      LegendTitle legend = chart.getLegend();
      legend.setBackgroundPaint(currentBgColor);
      legend.setItemFont(new Font(fontName, Font.PLAIN, fontSize));
      legend.setFrame(BlockBorder.NONE);
      if (config.getLegendPosition() != null) {
        switch (config.getLegendPosition().toUpperCase()) {
          case "RIGHT":
            legend.setPosition(RectangleEdge.RIGHT);
            break;
          case "TOP":
            legend.setPosition(RectangleEdge.TOP);
            break;
          case "LEFT":
            legend.setPosition(RectangleEdge.LEFT);
            break;
          default:
            legend.setPosition(RectangleEdge.BOTTOM);
            break;
        }
      }
    }

    return chart;
  }

  private Color decodeColor(String hexStr, Color fallback) {
    if (hexStr == null || hexStr.isEmpty())
      return fallback;
    try {
      return Color.decode(hexStr.startsWith("#") ? hexStr : "#" + hexStr);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}