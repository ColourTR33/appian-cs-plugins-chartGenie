package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.Color;
import java.util.Locale;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared chart styling utilities used across all strategy implementations.
 * Eliminates duplicate decodeColor and legend positioning logic (CWE-390, Similar code).
 */
final class ChartStyleUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ChartStyleUtils.class);
  private static final Color DEFAULT_BG = Color.WHITE;

  private ChartStyleUtils() {
  }

  /**
   * Decodes a hex color string. Returns fallback on null/empty/invalid input.
   * Logs a warning on invalid input (CWE-390 — no silent swallowing).
   */
  static Color decodeColor(String hexStr, Color fallback) {
    if (hexStr == null || hexStr.isEmpty())
      return fallback;
    try {
      return Color.decode(hexStr.startsWith("#") ? hexStr : "#" + hexStr);
    } catch (NumberFormatException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Invalid hex color '{}', using fallback: {}", hexStr, e.getMessage());
      }
      return fallback;
    }
  }

  /**
   * Decodes a background color from config. Returns WHITE on null/empty/invalid.
   */
  static Color decodeBackgroundColor(String bgHex) {
    if (bgHex == null || bgHex.isEmpty())
      return DEFAULT_BG;
    try {
      return Color.decode("#" + bgHex.replace("#", ""));
    } catch (NumberFormatException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Invalid background color '{}', using WHITE: {}", bgHex, e.getMessage());
      }
      return DEFAULT_BG;
    }
  }

  /**
   * Applies legend position from a string value (NONE, RIGHT, TOP, LEFT, BOTTOM).
   * Uses Locale.ROOT for case-insensitive comparison (i18n fix).
   */
  static void applyLegendPosition(JFreeChart chart, String position, Color bgColor, java.awt.Font font) {
    if (position == null)
      return;
    String pos = position.toUpperCase(Locale.ROOT);
    if ("NONE".equals(pos)) {
      chart.removeLegend();
      return;
    }
    LegendTitle legend = chart.getLegend();
    if (legend == null)
      return;
    legend.setBackgroundPaint(bgColor);
    if (font != null)
      legend.setItemFont(font);
    legend.setFrame(BlockBorder.NONE);
    switch (pos) {
      case "RIGHT":
        legend.setPosition(RectangleEdge.RIGHT);
        break;
      case "TOP":
        legend.setPosition(RectangleEdge.TOP);
        break;
      case "LEFT":
        legend.setPosition(RectangleEdge.LEFT);
        break;
      default:
        legend.setPosition(RectangleEdge.BOTTOM);
        break;
    }
  }
}
