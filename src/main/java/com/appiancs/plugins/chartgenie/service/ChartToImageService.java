package com.appiancs.plugins.chartgenie.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class ChartToImageService {
  private final ChartConfiguration request;
  private static final Logger LOG = Logger.getLogger(ChartToImageService.class);

  public ChartToImageService(ChartConfiguration request) {
    this.request = request;
  }

  // Render the chart image
  public void renderChart(ChartConfiguration config) throws IOException {

    // 1. Get the correct chart logic
    ChartGeneratorStrategy strategy = ChartStrategyFactory.getStrategy(config.getChartTypeEnum());

    // 2. Generate the chart object
    JFreeChart chart = strategy.generate(config);

    // 3. Create a temporary file to hold the image
    File tempFile = File.createTempFile("chart_gen_", ".png");

    // 4. Render JFreeChart to PNG
    ChartUtilities.saveChartAsPNG(tempFile, chart, config.getWidth(), config.getHeight());

    try {
      uploadChartToAppian(tempFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void uploadChartToAppian(File chartFile) throws Exception {
    com.appiancorp.suiteapi.knowledge.Document outputChartImage = new com.appiancorp.suiteapi.knowledge.Document();
    outputChartImage.setName(request.targetChartDocumentName.trim());
    if (request.targetChartDocumentDesc != null) {
      outputChartImage.setDescription(request.targetChartDocumentDesc.trim());
    }
    outputChartImage.setExtension("png");
    outputChartImage.setParent(request.targetChartFolder);

    request.newChartDocumentCreated = request.cs.create(outputChartImage, ContentConstants.UNIQUE_NONE);
    com.appiancorp.suiteapi.knowledge.Document finalDoc = request.cs.download(request.newChartDocumentCreated,
      ContentConstants.VERSION_CURRENT,
      false)[0];

    try (OutputStream outputStream = finalDoc.getOutputStream()) {
      Files.copy(chartFile.toPath(), outputStream);
    }
    if (LOG.isEnabledFor(org.apache.log4j.Level.INFO)) {
      LOG.info("PDF successfully uploaded to Appian as document ID: " + request.newChartDocumentCreated);
    }
  }
}