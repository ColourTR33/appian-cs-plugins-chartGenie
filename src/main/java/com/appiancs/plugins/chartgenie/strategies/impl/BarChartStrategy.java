package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class BarChartStrategy implements ChartGeneratorStrategy {

  private static final Paint[] BIA_PALETTE = {
    Color.decode("#00395D"), // Barclays Dark Blue (Moved to FIRST position)
    Color.decode("#00AEEF"), // Barclays Light Blue
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
        String category = (data.getCategory() != null) ? data.getCategory() : "Unknown";

        // --- MULTIPLE BARS RESTORED ---
        // If a series is provided in JSON, use it (Groups multiple bars).
        // If not, fallback to 'category' so single bars still get unique legend colors!
        String series = (data.getSeries() != null && !data.getSeries().isEmpty()) ? data.getSeries() : category;
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

    Shape largeLegendBox = new Ellipse2D.Double(-15, -15, 30, 30);

    // Apply Barclays Colors and Custom Legend Shape
    plot.setDrawingSupplier(new DefaultDrawingSupplier(
      BIA_PALETTE,
      DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
      new Shape[] { largeLegendBox }));

    // Transparent background
    plot.setBackgroundPaint(new Color(0, 0, 0, 0));
    chart.setBackgroundPaint(null);
    plot.setOutlineVisible(false);

    // --- Y-AXIS GRIDLINES FIX ---
    plot.setRangeGridlinesVisible(true);
    plot.setRangeGridlinePaint(Color.GRAY); // Darker color to show up on transparent background
    // Add a professional, highly visible dashed line for the grid
    plot.setRangeGridlineStroke(
      new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] { 10.0f, 6.0f }, 0.0f));

    Font largeAxisFont = new Font("SansSerif", Font.PLAIN, 30);

    // --- X-AXIS RESTORED ---
    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setTickLabelFont(largeAxisFont);
    domainAxis.setTickLabelsVisible(true); // Turn text back on so grouped bars can be identified
    domainAxis.setTickMarksVisible(true);

    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setTickLabelFont(largeAxisFont);
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits()); // No Decimals
    rangeAxis.setAxisLineVisible(true);
    rangeAxis.setTickMarksVisible(true);

    // --- FLAT DESIGN ---
    BarRenderer renderer = (BarRenderer) plot.getRenderer();
    renderer.setBarPainter(new StandardBarPainter()); // Removes outdated 3D gloss effect
    renderer.setDrawBarOutline(false);
    renderer.setShadowVisible(false);
    renderer.setItemMargin(0.02); // Adds a tiny gap between multiple bars in the same category
    renderer.setMaximumBarWidth(1.00);

    LegendTitle legend = chart.getLegend();
    if (legend != null) {
      legend.setBackgroundPaint(null);
      legend.setItemFont(new Font("SansSerif", Font.BOLD, 45));
    }

    return chart;
  }
}