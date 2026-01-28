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

public class AreaChartStrategy implements ChartGeneratorStrategy {

  private static final Color BG_GREY = new Color(242, 242, 242);
  private static final Color CYAN_TRANSPARENT = new Color(0, 184, 212, 180); // Transparent Cyan

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    List<String> categories = config.getCategories();
    List<Number> values = config.getValues();

    if (categories != null && values != null) {
      for (int i = 0; i < categories.size(); i++) {
        if (i < values.size()) {
          dataset.addValue(values.get(i), "Volume", categories.get(i));
        }
      }
    }

    JFreeChart chart = ChartFactory.createAreaChart(
      config.getTitle(), "", "", dataset,
      PlotOrientation.VERTICAL, false, true, false);

    chart.setBackgroundPaint(BG_GREY);
    chart.setBorderVisible(false);
    chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 18));
    chart.getTitle().setBackgroundPaint(BG_GREY);

    CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(BG_GREY);
    plot.setOutlineVisible(false);
    plot.setRangeGridlinePaint(Color.WHITE);
    plot.setDomainGridlinesVisible(false);

    // Axis
    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
    domainAxis.setLowerMargin(0.0); // Flush to edges
    domainAxis.setUpperMargin(0.0);

    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
    rangeAxis.setAutoRangeIncludesZero(true);

    // Renderer
    AreaRenderer renderer = (AreaRenderer) plot.getRenderer();
    renderer.setSeriesPaint(0, CYAN_TRANSPARENT);

    return chart;
  }
}