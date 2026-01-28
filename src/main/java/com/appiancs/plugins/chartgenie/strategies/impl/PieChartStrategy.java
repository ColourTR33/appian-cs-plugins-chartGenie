package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class PieChartStrategy implements ChartGeneratorStrategy {

  private static final Color BG_GREY = new Color(242, 242, 242);
  // Corporate Palette
  private static final Color[] PALETTE = {
    new Color(30, 60, 150), // Dark Blue
    new Color(0, 184, 212), // Cyan
    new Color(160, 160, 160) // Grey
  };

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    DefaultPieDataset dataset = new DefaultPieDataset();
    List<String> categories = config.getCategories();
    List<Number> values = config.getValues();

    if (categories != null && values != null) {
      for (int i = 0; i < categories.size(); i++) {
        if (i < values.size())
          dataset.setValue(categories.get(i), values.get(i));
      }
    }

    JFreeChart chart = ChartFactory.createPieChart(
      config.getTitle(), dataset, true, true, false);

    // Styling
    chart.setBackgroundPaint(BG_GREY);
    chart.setBorderVisible(false);
    chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 18));
    chart.getTitle().setBackgroundPaint(BG_GREY);

    PiePlot plot = (PiePlot) chart.getPlot();
    plot.setBackgroundPaint(BG_GREY);
    plot.setOutlineVisible(false); // No border around the whole circle
    plot.setShadowPaint(null);
    plot.setLabelGenerator(null); // Cleaner look without external labels
    plot.setSectionOutlinesVisible(true);

    // Apply Palette & Outlines per Slice
    BasicStroke whiteLine = new BasicStroke(2.0f);

    for (int i = 0; i < dataset.getItemCount(); i++) {
      Comparable key = dataset.getKey(i);

      // 1. Set Fill Color
      plot.setSectionPaint(key, PALETTE[i % PALETTE.length]);

      // 2. Set White Separator Line (The Fix)
      plot.setSectionOutlinePaint(key, Color.WHITE);
      plot.setSectionOutlineStroke(key, whiteLine);
    }

    // Legend Styling
    if (chart.getLegend() != null) {
      chart.getLegend().setBackgroundPaint(BG_GREY);
      chart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 12));
      chart.getLegend().setFrame(org.jfree.chart.block.BlockBorder.NONE);
    }

    return chart;
  }
}