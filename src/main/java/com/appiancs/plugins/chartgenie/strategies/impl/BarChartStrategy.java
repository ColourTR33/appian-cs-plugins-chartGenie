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
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class BarChartStrategy implements ChartGeneratorStrategy {

  private static final Color BG_GREY = new Color(242, 242, 242);
  private static final Color DARK_BLUE = new Color(30, 60, 150);

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    List<String> categories = config.getCategories();
    List<Number> values = config.getValues();

    if (categories != null && values != null) {
      for (int i = 0; i < categories.size(); i++) {
        if (i < values.size()) {
          Number val = values.get(i);
          double doubleVal = (val != null) ? val.doubleValue() : 0.0;
          dataset.addValue(doubleVal, "Series 1", categories.get(i));
        }
      }
    }

    JFreeChart chart = ChartFactory.createBarChart(
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

    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));

    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
    rangeAxis.setAutoRangeIncludesZero(true);

    BarRenderer renderer = (BarRenderer) plot.getRenderer();
    renderer.setBarPainter(new StandardBarPainter());
    renderer.setShadowVisible(false);
    renderer.setDrawBarOutline(false);
    renderer.setSeriesPaint(0, DARK_BLUE);

    return chart;
  }
}