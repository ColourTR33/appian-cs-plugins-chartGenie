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
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class AreaChartStrategy implements ChartGeneratorStrategy {

  private static final Color COLOR_GRIDLINES = new Color(220, 220, 220);
  private static final Color COLOR_PRIMARY_DEFAULT = new Color(0, 184, 212);
  private static final String DEFAULT_FONT = "SansSerif";
  private static final int ALPHA_TRANSPARENCY = 180;

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    if (config.getMultiSeriesData() != null && !config.getMultiSeriesData().isEmpty()) {
      for (ChartDataPoint point : config.getMultiSeriesData()) {
        dataset.addValue(point.getValue(), point.getSeries(), point.getCategory());
      }
    } else if (config.getCategories() != null && config.getValues() != null) {
      String seriesName = (config.getSeriesName() != null && !config.getSeriesName().isEmpty()) ? config.getSeriesName() : "Data";
      int size = Math.min(config.getCategories().size(), config.getValues().size());
      for (int i = 0; i < size; i++) {
        dataset.addValue(config.getValues().get(i), seriesName, config.getCategories().get(i));
      }
    }

    JFreeChart chart = ChartFactory.createAreaChart(config.getTitle(), "", "", dataset, PlotOrientation.VERTICAL, true, false, false);

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

    Color primaryColor = decodeColor(config.getPrimaryColor(), COLOR_PRIMARY_DEFAULT);
    Color transparentColor = new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), ALPHA_TRANSPARENCY);

    AreaRenderer renderer = (AreaRenderer) plot.getRenderer();
    renderer.setSeriesPaint(0, transparentColor);

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