package com.appiancs.plugins.chartgenie;

import java.io.File;
import java.util.Arrays;
import java.util.List;
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
import com.appiancs.plugins.chartgenie.utils.DocumentUtils;

@PaletteInfo(paletteCategory = "Document Generation", palette = "ChartGenie Services")
public class GenerateChartImage extends BaseSmartService {

  private String chartType;
  private String[] dataCategories;
  private Double[] dataValues;
  private String primaryColor;
  private Long targetFolderId;
  private String targetName;
  private Long newChartDocumentId;

  public GenerateChartImage(SmartServiceContext context, ContentService contentService) {
    super(contentService);
  }

  @Override
  public void run() throws SmartServiceException {
    File chartFile = null;

    try {
      log.info("Starting Single Chart Image Generation...");

      ChartConfiguration config = new ChartConfiguration();
      config.setChartType(chartType);

      if (dataCategories != null)
        config.setCategories(Arrays.asList(dataCategories));
      if (dataValues != null) {
        List<Number> valuesList = Arrays.stream(dataValues).collect(Collectors.toList());
        config.setValues(valuesList);
      }

      config.setPrimaryColor(primaryColor);
      config.setWidth(800);
      config.setHeight(600);
      config.setTitle(targetName);

      ChartGenerationService service = new ChartGenerationService();
      chartFile = service.generateChartImage(config);

      // Upload using Utility
      this.newChartDocumentId = DocumentUtils.uploadDocument(contentService, chartFile, targetName, targetFolderId, "png");

      log.info("Chart Image Created Successfully. ID: " + newChartDocumentId);

    } catch (Exception e) {
      handleException(e, "Failed to generate chart image");
      this.newChartDocumentId = null;
    } finally {
      if (chartFile != null)
        chartFile.delete();
    }
  }

  @Input(required = Required.ALWAYS)
  public void setChartType(String chartType) {
    this.chartType = chartType;
  }

  @Input(required = Required.ALWAYS)
  public void setDataCategories(String[] dataCategories) {
    this.dataCategories = dataCategories;
  }

  @Input(required = Required.ALWAYS)
  public void setDataValues(Double[] dataValues) {
    this.dataValues = dataValues;
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