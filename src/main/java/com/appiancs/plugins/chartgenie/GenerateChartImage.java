package com.appiancs.plugins.chartgenie;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.SmartServiceContext;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancs.plugins.chartgenie.base.BaseSmartService;
import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.service.ChartGenerationService;
import com.appiancs.plugins.chartgenie.service.DocumentUtils;

@PaletteInfo(paletteCategory = "Document Generation", palette = "ChartGenie Services")
public class GenerateChartImage extends BaseSmartService {

  private String chartType;
  private String[] dataCategories;
  private Double[] dataValues;
  private String primaryColor;
  private Long targetFolderId;
  private String targetName;
  private Long newChartDocumentId;

  // Multi-series inputs
  private String[] seriesNames; // parallel to dataSeriesValues
  private String[] seriesCategories; // shared category labels
  private String multiSeriesJson; // JSON array of ChartDataPoint objects as alternative

  public GenerateChartImage(SmartServiceContext context, ContentService contentService) {
    super(contentService);
  }

  @Override
  public void run() throws SmartServiceException {
    File tempChartFile = null;

    try {
      log.info("Starting Single Chart Image Generation...");

      // Input validation
      validateInputs();

      ChartConfiguration config = new ChartConfiguration();
      config.setChartType(sanitizeChartType(chartType));
      config.setPrimaryColor(sanitizeColor(primaryColor));
      config.setWidth(600);
      config.setHeight(600);
      config.setTitle(sanitizeTitle(targetName));

      // Multi-series via JSON payload takes highest priority
      if (multiSeriesJson != null && !multiSeriesJson.trim().isEmpty()) {
        java.util.List<com.appiancs.plugins.chartgenie.dto.ChartDataPoint> points = parseMultiSeriesJson(multiSeriesJson);
        config.setMultiSeriesData(points);
      } else {
        // Single-series (legacy flat arrays)
        if (dataCategories != null) {
          config.setCategories(Arrays.asList(sanitizeCategories(dataCategories)));
        }
        if (dataValues != null) {
          java.util.List<Number> valuesList = Arrays.stream(dataValues)
            .filter(val -> val != null && !val.isNaN() && !val.isInfinite())
            .collect(Collectors.toList());
          config.setValues(valuesList);
        }
      }

      ChartGenerationService service = new ChartGenerationService();

      // 1. Get the raw image bytes from our updated service
      byte[] chartBytes = service.generateChartImage(config);

      // 2. Create NIO temp file anchored to real temp dir (CWE-22/23)
      Path safeTempDir = Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
      Path tempChartPath = Files.createTempFile(safeTempDir, "genie_chart_smartservice_", ".png");
      tempChartFile = tempChartPath.toFile();

      // 3. Write bytes to safe path
      Files.write(tempChartPath, chartBytes);

      // 4. Upload using your existing Utility (unchanged!)
      this.newChartDocumentId = DocumentUtils.uploadDocument(contentService, tempChartFile,
        sanitizeFileName(targetName), targetFolderId, "png");

      log.info("Chart Image Created Successfully. ID: {}", newChartDocumentId);

    } catch (IllegalArgumentException e) {
      handleException(e, e.getMessage());
      this.newChartDocumentId = null;
    } catch (Exception e) {
      log.error("Failed to generate chart image", e);
      handleException(e, "Failed to generate chart image");
      this.newChartDocumentId = null;
    } finally {
      // 5. The temp file is safely deleted in the finally block of the SAME method
      if (tempChartFile != null && tempChartFile.exists()) {
        if (!tempChartFile.delete()) {
          log.warn("Failed to delete temp chart file: {}", tempChartFile.getAbsolutePath());
        }
      }
    }
  }

  private void validateInputs() {
    if (chartType == null || chartType.trim().isEmpty()) {
      throw new IllegalArgumentException("Chart type is required");
    }
    if (targetFolderId == null) {
      throw new IllegalArgumentException("Target folder is required");
    }
    if (targetName == null || targetName.trim().isEmpty()) {
      throw new IllegalArgumentException("Target name is required");
    }
    // Must have either multi-series JSON or flat category/value arrays
    boolean hasMultiSeries = multiSeriesJson != null && !multiSeriesJson.trim().isEmpty();
    boolean hasFlatArrays = (dataCategories != null && dataCategories.length > 0) && (dataValues != null && dataValues.length > 0);
    if (!hasMultiSeries && !hasFlatArrays) {
      throw new IllegalArgumentException(
        "Either multiSeriesJson or both dataCategories and dataValues are required");
    }
    if (!hasMultiSeries && dataCategories.length != dataValues.length) {
      throw new IllegalArgumentException("Categories and values must have the same length");
    }
  }

  private java.util.List<com.appiancs.plugins.chartgenie.dto.ChartDataPoint> parseMultiSeriesJson(
    String json) {
    try {
      com.google.gson.Gson gson = new com.google.gson.Gson();
      com.google.gson.reflect.TypeToken<java.util.List<com.appiancs.plugins.chartgenie.dto.ChartDataPoint>> token = new com.google.gson.reflect.TypeToken<java.util.List<com.appiancs.plugins.chartgenie.dto.ChartDataPoint>>() {
      };
      return gson.fromJson(json, token.getType());
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid multiSeriesJson: " + e.getMessage(), e);
    }
  }

  private String sanitizeChartType(String type) {
    if (type == null)
      return "BAR";

    String clean = type.toUpperCase(java.util.Locale.ROOT).trim();
    // Validate against allowed chart types
    String[] allowedTypes = { "BAR", "COLUMN", "LINE", "AREA", "PIE", "DONUT", "STACKED", "SCATTER" };
    for (String allowed : allowedTypes) {
      if (allowed.equals(clean)) {
        return clean;
      }
    }
    return "BAR"; // Default fallback
  }

  private String[] sanitizeCategories(String[] categories) {
    if (categories == null)
      return new String[0];

    return Arrays.stream(categories)
      .map(this::sanitizeText)
      .toArray(String[]::new);
  }

  private String sanitizeColor(String color) {
    if (color == null || color.trim().isEmpty()) {
      return "1E3C96"; // Default blue
    }

    // Remove # if present and validate hex format
    String clean = color.replace("#", "").trim();
    if (clean.matches("^[0-9A-Fa-f]{6}$")) {
      return clean.toUpperCase(java.util.Locale.ROOT);
    }

    return "1E3C96"; // Default if invalid
  }

  private String sanitizeTitle(String title) {
    if (title == null)
      return "Chart";
    return sanitizeText(title);
  }

  private String sanitizeFileName(String name) {
    if (name == null)
      return "chart";

    return name.replaceAll("[.]{2,}", "")
      .replaceAll("[/\\\\:*?\"<>|]", "")
      .replaceAll("[\\r\\n\\t]", "")
      .trim();
  }

  private String sanitizeText(String text) {
    if (text == null)
      return "";

    // Remove control characters and potential injection vectors
    return text.replaceAll("[\\r\\n\\t]", " ")
      .replaceAll("[\\p{Cntrl}]", "")
      .replaceAll("[<>\"'&]", "")
      .trim();
  }

  @Input(required = Required.OPTIONAL)
  public void setMultiSeriesJson(String multiSeriesJson) {
    this.multiSeriesJson = multiSeriesJson;
  }

  @Input(required = Required.ALWAYS)
  public void setChartType(String chartType) {
    this.chartType = chartType;
  }

  @Input(required = Required.OPTIONAL)
  public void setDataCategories(String[] dataCategories) {
    this.dataCategories = dataCategories == null ? null : dataCategories.clone();
  }

  @Input(required = Required.OPTIONAL)
  public void setDataValues(Double[] dataValues) {
    this.dataValues = dataValues == null ? null : dataValues.clone();
  }

  @Input(required = Required.OPTIONAL)
  public void setPrimaryColor(String primaryColor) {
    this.primaryColor = primaryColor;
  }

  @Input(required = Required.ALWAYS)
  public void setTargetFolder(Long targetFolderId) {
    this.targetFolderId = targetFolderId;
  }

  @Input(required = Required.ALWAYS)
  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  @Input(required = Required.OPTIONAL)
  public Long getNewChartDocument() {
    return newChartDocumentId;
  }
}