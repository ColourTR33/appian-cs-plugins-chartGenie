package com.appiancs.plugins.chartgenie.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

/**
 * Service responsible for generating chart images from configuration.
 * Orchestrates the Strategy Factory and streams chart images to memory.
 */
public class ChartGenerationService {

  // FIXED: Correct SLF4J initialization
  private static final Logger LOG = LoggerFactory.getLogger(ChartGenerationService.class);

  // Defaults
  private static final int DEFAULT_WIDTH = 500;
  private static final int DEFAULT_HEIGHT = 300;

  // Safety Caps (Prevent OOM attacks or mistakes)
  private static final int MIN_DIMENSION = 100;
  private static final int MAX_DIMENSION = 2000;

  private final ChartStrategyFactory chartStrategyFactory;

  /**
   * No-arg constructor for production use — delegates to parameterized constructor
   * with a default ChartStrategyFactory instance.
   */
  public ChartGenerationService() {
    this(new ChartStrategyFactory());
  }

  /**
   * Parameterized constructor for dependency injection and testing.
   *
   * @param chartStrategyFactory the factory used to resolve chart strategies
   * @throws IllegalArgumentException if chartStrategyFactory is null
   */
  public ChartGenerationService(ChartStrategyFactory chartStrategyFactory) {
    if (chartStrategyFactory == null) {
      throw new IllegalArgumentException("chartStrategyFactory must not be null");
    }
    this.chartStrategyFactory = chartStrategyFactory;
  }

  public byte[] generateChartImage(ChartConfiguration config) throws IOException {
    if (config == null) {
      throw new IllegalArgumentException("Chart configuration cannot be null.");
    }

    // FIXED: Using SLF4J parameterized logging
    LOG.debug("Generating chart of type: {}", config.getChartType());

    int reqWidth = (config.getWidth() != null && config.getWidth() > 0) ? config.getWidth() : DEFAULT_WIDTH;
    int reqHeight = (config.getHeight() != null && config.getHeight() > 0) ? config.getHeight() : DEFAULT_HEIGHT;

    int finalWidth = Math.min(Math.max(reqWidth, MIN_DIMENSION), MAX_DIMENSION);
    int finalHeight = Math.min(Math.max(reqHeight, MIN_DIMENSION), MAX_DIMENSION);

    ChartGeneratorStrategy strategy = this.chartStrategyFactory.getStrategy(config.getChartType());
    JFreeChart chart = strategy.generate(config);

    if (chart == null) {
      throw new IllegalStateException("Strategy returned a null chart for type: " + config.getChartType());
    }

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ChartUtils.writeChartAsPNG(baos, chart, finalWidth, finalHeight);
      return baos.toByteArray();
    } catch (IOException e) {
      // FIXED: SLF4J error logging
      LOG.error("Failed to write chart image to byte array stream: {}", e.getMessage(), e);
      throw e;
    }
  }
}