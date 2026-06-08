package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.service.MonochromaticPaletteGenerator;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class AreaChartStrategy implements ChartGeneratorStrategy {

  private static final Color COLOR_GRIDLINES = new Color(220, 220, 220);
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

    Color currentBgColor = ChartStyleUtils.decodeBackgroundColor(config.getBackgroundColor());
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

    Paint[] palette = MonochromaticPaletteGenerator.resolve(config.getPrimaryColor());

    AreaRenderer renderer = (AreaRenderer) plot.getRenderer();
    for (int i = 0; i < dataset.getRowCount(); i++) {
      Color baseColor = (Color) palette[i % palette.length];
      Color transparentColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), ALPHA_TRANSPARENCY);
      renderer.setSeriesPaint(i, transparentColor);
    }

    ChartStyleUtils.applyLegendPosition(chart, config.getLegendPosition(), currentBgColor,
      new Font(fontName, Font.PLAIN, fontSize));

    return chart;
  }
}