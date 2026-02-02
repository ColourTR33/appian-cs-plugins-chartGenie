package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

/**
 * Strategy to generate a Donut (Ring) Chart.
 */
public class DonutChartStrategy implements ChartGeneratorStrategy {

  // Defaults
  private static final Color COLOR_BG_DEFAULT = Color.WHITE;
  private static final Color COLOR_PRIMARY_DEFAULT = new Color(30, 60, 150); // Deep Blue

  private static final String DEFAULT_FONT = "SansSerif";

  // Layout Constants
  private static final int FONT_SIZE_TITLE = 18;
  private static final int FONT_SIZE_LEGEND = 12;
  private static final float SEPARATOR_WIDTH = 2.0f;
  private static final double RING_DEPTH = 0.35; // 35% thickness

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    // 1. Prepare Dataset
    DefaultPieDataset dataset = new DefaultPieDataset();
    List<String> categories = config.getCategories();
    List<Number> values = config.getValues();

    if (categories != null && values != null) {
      int size = Math.min(categories.size(), values.size());
      for (int i = 0; i < size; i++) {
        if (values.get(i) != null) {
          dataset.setValue(categories.get(i), values.get(i));
        }
      }
    }

    // 2. Create Chart
    JFreeChart chart = ChartFactory.createRingChart(
      config.getTitle(),
      dataset,
      true, // Legend
      true, // Tooltips
      false // URLs
    );

    // 3. Apply Global Styling
    chart.setBackgroundPaint(COLOR_BG_DEFAULT);
    chart.setBorderVisible(false);

    // Resolve Font
    String fontName = (config.getFontFamily() != null) ? config.getFontFamily() : DEFAULT_FONT;

    // Title
    TextTitle title = chart.getTitle();
    title.setFont(new Font(fontName, Font.BOLD, FONT_SIZE_TITLE));
    title.setPaint(Color.BLACK);
    title.setBackgroundPaint(COLOR_BG_DEFAULT);

    // 4. Plot Styling
    RingPlot plot = (RingPlot) chart.getPlot();
    plot.setBackgroundPaint(COLOR_BG_DEFAULT);
    plot.setOutlineVisible(false);
    plot.setShadowPaint(null); // Flat design (no shadow)
    plot.setSectionDepth(RING_DEPTH); // Thicker ring for modern look

    // Separators
    plot.setSeparatorPaint(Color.WHITE);
    plot.setSeparatorStroke(new BasicStroke(SEPARATOR_WIDTH));

    // Hide labels on the chart itself
    plot.setLabelGenerator(null);

    // 5. Generate Palette & Apply Colors
    Color primaryColor = decodeColor(config.getPrimaryColor(), COLOR_PRIMARY_DEFAULT);
    Color[] palette = generateMonochromaticPalette(primaryColor, dataset.getItemCount());

    for (int i = 0; i < dataset.getItemCount(); i++) {
      Comparable key = dataset.getKey(i);
      plot.setSectionPaint(key, palette[i % palette.length]);
    }

    // 6. Legend Styling
    if (chart.getLegend() != null) {
      chart.getLegend().setBackgroundPaint(COLOR_BG_DEFAULT);
      chart.getLegend().setItemFont(new Font(fontName, Font.PLAIN, FONT_SIZE_LEGEND));
      chart.getLegend().setFrame(BlockBorder.NONE);
    }

    return chart;
  }

  /**
   * Generates a monochromatic palette (shades of the primary color).
   */
  private Color[] generateMonochromaticPalette(Color base, int count) {
    if (count <= 0) {
      return new Color[] { base };
    }

    Color[] palette = new Color[count];
    palette[0] = base;

    for (int i = 1; i < count; i++) {
      if (i == count - 1 && count > 2) {
        palette[i] = Color.LIGHT_GRAY;
      } else {
        palette[i] = lighten(palette[i - 1], 0.2);
      }
    }
    return palette;
  }

  private Color lighten(Color color, double fraction) {
    int r = color.getRed();
    int g = color.getGreen();
    int b = color.getBlue();
    int alpha = color.getAlpha();

    int rNew = (int) Math.min(255, r + (255 - r) * fraction);
    int gNew = (int) Math.min(255, g + (255 - g) * fraction);
    int bNew = (int) Math.min(255, b + (255 - b) * fraction);

    return new Color(rNew, gNew, bNew, alpha);
  }

  private Color decodeColor(String hexStr, Color fallback) {
    if (hexStr == null || hexStr.isEmpty()) {
      return fallback;
    }
    try {
      String cleanHex = hexStr.startsWith("#") ? hexStr : "#" + hexStr;
      return Color.decode(cleanHex);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}