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
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class StackedColumnStrategy implements ChartGeneratorStrategy {

  private static final Color BG_GREY = new Color(242, 242, 242);
  private static final Color DARK_BLUE = new Color(30, 60, 150);
  private static final Color CYAN = new Color(0, 184, 212);
  private static final Color GREY_DATA = new Color(160, 160, 160);

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    List<String> categories = config.getCategories();
    List<Number> values = config.getValues();

    // 1. ROBUST DATA POPULATION
    if (categories != null && values != null) {
      for (int i = 0; i < categories.size(); i++) {
        if (i < values.size()) {
          Number val = values.get(i);
          // Handle nulls gracefully to prevent crashes
          double doubleVal = (val != null) ? val.doubleValue() : 0.0;
          dataset.addValue(doubleVal, "Series 1", categories.get(i));
        }
      }
    }

    JFreeChart chart = ChartFactory.createStackedBarChart(
      config.getTitle(), "", "", dataset,
      PlotOrientation.VERTICAL, false, true, false);

    // 2. STYLING
    chart.setBackgroundPaint(BG_GREY);
    chart.setBorderVisible(false);

    // Title
    chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 18));
    chart.getTitle().setBackgroundPaint(BG_GREY);

    // Plot
    CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(BG_GREY);
    plot.setOutlineVisible(false);
    plot.setRangeGridlinePaint(Color.WHITE);
    plot.setDomainGridlinesVisible(false);

    // 3. AXIS FIXES (Ensure bars are visible)
    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
    domainAxis.setLowerMargin(0.05); // Give bars breathing room
    domainAxis.setUpperMargin(0.05);

    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    rangeAxis.setAutoRangeIncludesZero(true); // CRITICAL: Ensures bars start from 0

    // 4. RENDERER (Force Color)
    StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
    renderer.setBarPainter(new StandardBarPainter()); // Flat style
    renderer.setShadowVisible(false);
    renderer.setDrawBarOutline(false);

    // Force Series 0 to Dark Blue.
    // Note: For Stacked bars with 1 series, this paints everything blue.
    renderer.setSeriesPaint(0, DARK_BLUE);

    return chart;
  }
}