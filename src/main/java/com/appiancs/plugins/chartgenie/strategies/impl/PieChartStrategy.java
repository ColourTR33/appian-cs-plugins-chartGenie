package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

/**
 * Strategy to generate a Pie Chart with percentage labels and no internal title.
 */
public class PieChartStrategy implements ChartGeneratorStrategy {

  // Defaults
  private static final Color COLOR_BG_DEFAULT = Color.WHITE;
  private static final Color COLOR_PRIMARY_DEFAULT = new Color(30, 60, 150);

  private static final String DEFAULT_FONT = "SansSerif";

  // Layout Constants
  private static final int FONT_SIZE_LEGEND = 12;
  private static final int FONT_SIZE_LABELS = 10;
  private static final float SEPARATOR_WIDTH = 2.0f;

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
    // Pass NULL for title so it doesn't render in the image
    JFreeChart chart = ChartFactory.createPieChart(
      null,
      dataset,
      true, // Legend
      true, // Tooltips
      false // URLs
    );

    // 3. Apply Global Styling
    // Set to null (transparent) so it blends with the Word doc
    chart.setBackgroundPaint(null);
    chart.setBorderVisible(false);

    // Resolve Font
    String fontName = (config.getFontFamily() != null) ? config.getFontFamily() : DEFAULT_FONT;

    // 4. Plot Styling
    PiePlot plot = (PiePlot) chart.getPlot();
    plot.setBackgroundPaint(null); // Transparent plot background
    plot.setOutlineVisible(false);
    plot.setShadowPaint(null);

    // Separators
    BasicStroke separatorStroke = new BasicStroke(SEPARATOR_WIDTH);
    plot.setSectionOutlinesVisible(true);

    // --- REFINEMENT: PERCENTAGE LABELS ---
    // Format: "Key: 15%"
    // {0} = Key, {1} = Value, {2} = Percentage
    plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
      "{0}: {2}",
      new DecimalFormat("0"),
      new DecimalFormat("0%")));

    // Style the labels to be clean and readable
    plot.setLabelFont(new Font(fontName, Font.PLAIN, FONT_SIZE_LABELS));
    plot.setLabelBackgroundPaint(new Color(255, 255, 255, 200)); // Semi-transparent white box
    plot.setLabelOutlinePaint(null); // No border around label
    plot.setSimpleLabels(true); // Cleaner leader lines

    // 5. Generate Palette & Apply
    Color primaryColor = decodeColor(config.getPrimaryColor(), COLOR_PRIMARY_DEFAULT);
    Color[] palette = generateMonochromaticPalette(primaryColor, dataset.getItemCount());

    for (int i = 0; i < dataset.getItemCount(); i++) {
      Comparable key = dataset.getKey(i);

      plot.setSectionPaint(key, palette[i % palette.length]);
      plot.setSectionOutlinePaint(key, Color.WHITE);
      plot.setSectionOutlineStroke(key, separatorStroke);
    }

    // 6. Legend Styling
    if (chart.getLegend() != null) {
      chart.getLegend().setBackgroundPaint(null); // Transparent legend
      chart.getLegend().setItemFont(new Font(fontName, Font.PLAIN, FONT_SIZE_LEGEND));
      chart.getLegend().setFrame(BlockBorder.NONE);
    }

    return chart;
  }

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