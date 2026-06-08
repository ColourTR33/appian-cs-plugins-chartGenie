package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.DefaultXYDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.service.MonochromaticPaletteGenerator;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class ScatterPlotStrategy implements ChartGeneratorStrategy {

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    DefaultXYDataset dataset = new DefaultXYDataset();

    if (config.getMultiSeriesData() != null) {
      // Group data points by series
      java.util.Map<String, java.util.List<ChartDataPoint>> seriesMap = new java.util.HashMap<>();

      for (ChartDataPoint data : config.getMultiSeriesData()) {
        String series = (data.getSeries() != null && !data.getSeries().isEmpty())
          ? data.getSeries()
          : "Default Series";

        seriesMap.computeIfAbsent(series, k -> new java.util.ArrayList<>()).add(data);
      }

      // Convert each series to XY data
      for (java.util.Map.Entry<String, java.util.List<ChartDataPoint>> entry : seriesMap.entrySet()) {
        String seriesName = entry.getKey();
        java.util.List<ChartDataPoint> points = entry.getValue();

        double[][] data = new double[2][points.size()];

        for (int i = 0; i < points.size(); i++) {
          ChartDataPoint point = points.get(i);
          // Use category as X value (convert to numeric if possible)
          data[0][i] = parseNumericValue(point.getCategory(), i + 1);
          // Use value as Y value
          data[1][i] = (point.getValue() != null) ? point.getValue().doubleValue() : 0.0;
        }

        dataset.addSeries(seriesName, data);
      }
    }

    JFreeChart chart = ChartFactory.createScatterPlot(
      config.getTitle(),
      "Experience Level", // X-Axis Label
      "Performance Score", // Y-Axis Label
      dataset);

    XYPlot plot = chart.getXYPlot();

    // Enhanced marker shapes for different series
    Shape[] shapes = {
      new Ellipse2D.Double(-6, -6, 12, 12), // Circle
      new Polygon(new int[] { -6, 6, 0 }, new int[] { 4, 4, -8 }, 3), // Triangle
      new Rectangle(-6, -6, 12, 12), // Square
      new Polygon(new int[] { -8, -4, 4, 8, 4, -4 }, new int[] { 0, -6, -6, 0, 6, 6 }, 6) // Hexagon
    };

    Paint[] palette = MonochromaticPaletteGenerator.resolve(config.getPrimaryColor());

    // Apply color palette and custom shapes
    plot.setDrawingSupplier(new DefaultDrawingSupplier(
      palette,
      DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
      shapes));

    // Transparent background with subtle grid
    plot.setBackgroundPaint(new Color(0, 0, 0, 0));
    chart.setBackgroundPaint(null);
    plot.setOutlineVisible(false);

    // Enhanced grid lines
    plot.setDomainGridlinesVisible(true);
    plot.setRangeGridlinesVisible(true);
    plot.setDomainGridlinePaint(new Color(200, 200, 200, 128));
    plot.setRangeGridlinePaint(new Color(200, 200, 200, 128));
    plot.setDomainGridlineStroke(new BasicStroke(1.0f));
    plot.setRangeGridlineStroke(new BasicStroke(1.0f));

    Font largeAxisFont = new Font("SansSerif", Font.PLAIN, 24);
    Font labelFont = new Font("SansSerif", Font.BOLD, 28);

    // Enhanced X-Axis
    NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
    domainAxis.setTickLabelFont(largeAxisFont);
    domainAxis.setLabelFont(labelFont);
    domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    domainAxis.setAxisLineVisible(true);
    domainAxis.setTickMarksVisible(true);
    domainAxis.setAutoRangeIncludesZero(false);

    // Add padding to axes
    domainAxis.setLowerMargin(0.1);
    domainAxis.setUpperMargin(0.1);

    // Enhanced Y-Axis
    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setTickLabelFont(largeAxisFont);
    rangeAxis.setLabelFont(labelFont);
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    rangeAxis.setAxisLineVisible(true);
    rangeAxis.setTickMarksVisible(true);
    rangeAxis.setAutoRangeIncludesZero(false);

    rangeAxis.setLowerMargin(0.1);
    rangeAxis.setUpperMargin(0.1);

    // Enhanced renderer for scatter points
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
    renderer.setDefaultLinesVisible(false); // Only show points, no lines
    renderer.setDefaultShapesVisible(true);
    renderer.setDefaultShapesFilled(true);

    // Add tooltips (useful for debugging/development)
    renderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator(
      "{0}: ({1}, {2})", NumberFormat.getNumberInstance(), NumberFormat.getNumberInstance()));

    // Make points larger and add outline for better visibility
    for (int i = 0; i < dataset.getSeriesCount(); i++) {
      renderer.setSeriesShape(i, shapes[i % shapes.length]);
      renderer.setSeriesOutlinePaint(i, Color.WHITE);
      renderer.setSeriesOutlineStroke(i, new BasicStroke(2.0f));
    }

    plot.setRenderer(renderer);

    // Enhanced legend
    LegendTitle legend = chart.getLegend();
    if (legend != null) {
      legend.setBackgroundPaint(null);
      legend.setItemFont(new Font("SansSerif", Font.BOLD, 32));
      legend.setFrame(org.jfree.chart.block.BlockBorder.NONE);

      // Position legend at bottom
      legend.setPosition(org.jfree.chart.ui.RectangleEdge.BOTTOM);
    }

    return chart;
  }

  /**
   * Enhanced numeric parsing with better error handling
   */
  private double parseNumericValue(String value, int fallbackIndex) {
    if (value == null || value.trim().isEmpty())
      return fallbackIndex;
    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      String numericPart = value.replaceAll("[^0-9.]", "");
      if (!numericPart.isEmpty()) {
        try {
          return Double.parseDouble(numericPart);
        } catch (NumberFormatException ex) {
          org.slf4j.LoggerFactory.getLogger(ScatterPlotStrategy.class)
            .debug("Could not parse numeric value '{}', using index {}", value, fallbackIndex);
        }
      }
      return fallbackIndex;
    }
  }
}