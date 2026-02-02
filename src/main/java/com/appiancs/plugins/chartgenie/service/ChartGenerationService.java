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

  /**
   * Generates a PNG image file based on the provided configuration.
   *
   * @param config
   *          The chart configuration object.
   * @return A temporary File object containing the PNG image.
   * @throws IOException
   *           If disk write fails.
   * @throws IllegalArgumentException
   *           If configuration is invalid.
   */
  public File generateChartImage(ChartConfiguration config) throws IOException {
    if (config == null) {
      throw new IllegalArgumentException("Chart configuration cannot be null.");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Generating chart of type: " + config.getChartType());
    }

    // 1. Validate Dimensions with Safety Caps
    // We enforce a range [100px, 2000px] to prevent tiny unreadable charts
    // or massive images that crash the server heap.
    int reqWidth = (config.getWidth() > 0) ? config.getWidth() : DEFAULT_WIDTH;
    int reqHeight = (config.getHeight() > 0) ? config.getHeight() : DEFAULT_HEIGHT;

    int finalWidth = Math.min(Math.max(reqWidth, MIN_DIMENSION), MAX_DIMENSION);
    int finalHeight = Math.min(Math.max(reqHeight, MIN_DIMENSION), MAX_DIMENSION);

    // 2. Get Strategy
    // The Factory handles fallback logic (e.g. unknown type -> Bar Chart)
    ChartGeneratorStrategy strategy = ChartStrategyFactory.getStrategy(config.getChartType());

    // 3. Generate Chart Object
    JFreeChart chart = strategy.generate(config);

    if (chart == null) {
      throw new IllegalStateException("Strategy returned a null chart for type: " + config.getChartType());
    }

    // 4. Save to Temporary File
    // We use a prefix ensuring uniqueness in the OS temp directory
    File outputFile = File.createTempFile("genie_chart_", ".png");

    try {
      // Write the chart as a PNG to the temp file
      ChartUtils.saveChartAsPNG(outputFile, chart, finalWidth, finalHeight);
    } catch (IOException e) {
      LOG.error("Failed to write chart image to disk: " + outputFile.getAbsolutePath(), e);
      // Clean up the empty file if write failed
      if (outputFile.exists()) {
        outputFile.delete();
      }
      throw e; // Re-throw so the main service knows to abort
    }

    return outputFile;
  }
}