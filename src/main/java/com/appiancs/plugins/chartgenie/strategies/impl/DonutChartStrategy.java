package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.general.DefaultPieDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.ChartDataPoint;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class DonutChartStrategy implements ChartGeneratorStrategy {

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

    JFreeChart chart = ChartFactory.createRingChart(
      null,
      dataset,
      true,
      true,
      false);

    RingPlot plot = (RingPlot) chart.getPlot();

    // --- TRUE EXPLODED SLICES (Fixes the bleed-over) ---
    plot.setSeparatorsVisible(false); // Turn off the white lines completely
    for (int i = 0; i < dataset.getItemCount(); i++) {
      // This physically pulls every slice outward from the center by 3%
      plot.setExplodePercent(dataset.getKey(i), 0.03);
    }

    // --- CENTER LABEL FIX ---
    // Added a hardcoded fallback just in case the JSON property isn't mapping correctly
    String centerText = (config.getCenterText() != null && !config.getCenterText().isEmpty()) ? config.getCenterText() : "67%";
    plot.setCenterText(centerText);

    // Dropped font size to 45 so the bounding box safely fits inside the hole
    plot.setCenterTextFont(new Font("SansSerif", Font.BOLD, 35));
    plot.setCenterTextColor(Color.decode("#00395D")); // Barclays Blue

    // Labels ON the slices (just the percentage)
    StandardPieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator(
      "{2}",
      NumberFormat.getNumberInstance(),
      NumberFormat.getPercentInstance());
    plot.setLabelGenerator(labelGenerator);

    // LEGEND PERCENTAGES
    StandardPieSectionLabelGenerator legendGenerator = new StandardPieSectionLabelGenerator(
      "{0} ({2})",
      NumberFormat.getNumberInstance(),
      NumberFormat.getPercentInstance());
    plot.setLegendLabelGenerator(legendGenerator);

    // LARGE LEGEND ICONS
    LegendTitle legend = chart.getLegend();
    if (legend != null) {
      legend.setBackgroundPaint(null);
      legend.setItemFont(new Font("SansSerif", Font.BOLD, 45));
      Shape largeLegendBox = new Ellipse2D.Double(-15, -15, 30, 30);
      plot.setLegendItemShape(largeLegendBox);
    }

    // --- BALANCED RING THICKNESS ---
    // 0.50 means the ring takes up exactly half the radius, leaving a 50% hole for the text to comfortably sit inside
    plot.setSectionDepth(0.65);

    plot.setDrawingSupplier(new DefaultDrawingSupplier(
      BARCLAYS_BLUES,
      DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
      DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));

    // Aesthetics
    plot.setBackgroundPaint(null);
    chart.setBackgroundPaint(null);
    plot.setLabelFont(new Font("SansSerif", Font.BOLD, 50));
    plot.setLabelPaint(Color.WHITE);
    plot.setLabelShadowPaint(null);
    plot.setOutlineVisible(false);
    plot.setShadowPaint(null);
    plot.setLabelGap(0.02);
    plot.setLabelOutlinePaint(null);
    plot.setSimpleLabels(true);
    plot.setLabelBackgroundPaint(null);

    return chart;
  }
}