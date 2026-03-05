package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.general.DefaultPieDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class PieChartStrategy implements ChartGeneratorStrategy {

  // Strictly Barclays Blue Hues
  private static final Paint[] BARCLAYS_BLUES = {
    Color.decode("#00395D"), // Dark Blue
    Color.decode("#00AEEF"), // Light Blue
    Color.decode("#00B0CA"), // Cyan
    Color.decode("#005A8C"), // Mid Blue
    Color.decode("#80DFFF") // Pale Blue
  };

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    DefaultPieDataset dataset = new DefaultPieDataset();

    if (config.getMultiSeriesData() != null) {
      for (ChartDataPoint data : config.getMultiSeriesData()) {
        String category = (data.getCategory() != null) ? data.getCategory() : "Unknown";
        Number value = (data.getValue() != null) ? data.getValue() : 0;
        dataset.setValue(category, value);
      }
    }

    // CRITICAL FIX: Pass 'null' for the title to prevent it from rendering inside the image
    JFreeChart chart = ChartFactory.createPieChart(
      null,
      dataset,
      true,
      true,
      false);

    PiePlot plot = (PiePlot) chart.getPlot();

    plot.setDrawingSupplier(new DefaultDrawingSupplier(
      BARCLAYS_BLUES,
      DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));

    // CRITICAL FIX: Set Backgrounds to NULL to make the image transparent
    plot.setBackgroundPaint(null);
    chart.setBackgroundPaint(null);
    LegendTitle legend = chart.getLegend();
    if (legend != null) {
      legend.setBackgroundPaint(null); // Transparent legend background
      legend.setItemFont(new Font("SansSerif", Font.BOLD, 50));
      Shape largeLegendBox = new Ellipse2D.Double(-15, -15, 30, 30);
      plot.setLegendItemShape(largeLegendBox);
    }
    ;

    StandardPieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator(
      "{2}",
      NumberFormat.getNumberInstance(),
      NumberFormat.getPercentInstance());
    plot.setLabelGenerator(labelGenerator);

    plot.setLabelFont(new Font("SansSerif", Font.BOLD, 50));
    plot.setLabelPaint(Color.WHITE);
    plot.setLabelShadowPaint(null);
    plot.setLabelBackgroundPaint(null);
    plot.setOutlineVisible(false);
    plot.setShadowPaint(null);
    plot.setLabelGap(0.02);
    plot.setLabelOutlinePaint(null);
    plot.setSimpleLabels(true);

    return chart;
  }
}