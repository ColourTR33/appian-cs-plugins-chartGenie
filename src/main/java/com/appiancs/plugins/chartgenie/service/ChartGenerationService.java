package com.appiancs.plugins.chartgenie.service;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

/**
 * Service responsible for generating chart images from configuration.
 * Orchestrates the Strategy Factory and handles file I/O safely.
 */
public class ChartGenerationService {

  private static final Logger LOG = Logger.getLogger(ChartGenerationService.class);

  // Defaults
  private static final int DEFAULT_WIDTH = 500;
  private static final int DEFAULT_HEIGHT = 300;

  // Safety Caps (Prevent OOM attacks or mistakes)
  private static final int MIN_DIMENSION = 100;
  private static final int MAX_DIMENSION = 2000;

  public File generateChartImage(ChartConfiguration config) throws IOException {
    if (config == null) {
      throw new IllegalArgumentException("Chart configuration cannot be null.");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Generating chart of type: " + config.getChartType());
    }

    // CRITICAL FIX: Explicit null checks to prevent unboxing NullPointerExceptions
    int reqWidth = (config.getWidth() != null && config.getWidth() > 0) ? config.getWidth() : DEFAULT_WIDTH;
    int reqHeight = (config.getHeight() != null && config.getHeight() > 0) ? config.getHeight() : DEFAULT_HEIGHT;

    int finalWidth = Math.min(Math.max(reqWidth, MIN_DIMENSION), MAX_DIMENSION);
    int finalHeight = Math.min(Math.max(reqHeight, MIN_DIMENSION), MAX_DIMENSION);

    ChartGeneratorStrategy strategy = ChartStrategyFactory.getStrategy(config.getChartType());
    JFreeChart chart = strategy.generate(config);

    if (chart == null) {
      throw new IllegalStateException("Strategy returned a null chart for type: " + config.getChartType());
    }

    File outputFile = File.createTempFile("genie_chart_", ".png");

    try {
      ChartUtils.saveChartAsPNG(outputFile, chart, finalWidth, finalHeight);
    } catch (IOException e) {
      LOG.error("Failed to write chart image to disk: " + outputFile.getAbsolutePath(), e);
      if (outputFile.exists()) {
        outputFile.delete();
      }
      throw e;
    }

    return outputFile;
  }
}