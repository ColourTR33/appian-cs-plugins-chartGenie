package com.appiancs.plugins.chartgenie;

import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.framework.Unattended;
import com.appiancs.plugins.chartgenie.base.BaseSmartService;
import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.service.ChartToImageService;

// DESCRIPTION: Takes data arrays and styling configurations and outputs an Appian Document
@Unattended
@Order({
  "ChartType",
  "DataCategories",
  "DataValues",
  "PrimaryColour",
  "SecondaryColour",
  "CentreText"
})
public class ChartToImage extends BaseSmartService {
  private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(ChartToImage.class);

  public ChartToImage(ContentService cs) {
    super(cs, LOG);
  }

  public void run() throws SmartServiceException {

  ChartConfiguration chartConfig = new ChartConfiguration();
  chartConfig.cs = super.cs;

    //  TODO
    //1. Wire in chart configuration options

    ChartToImageService converter = new ChartToImageService(chartConfig);
//    this.targetChartDocumentCreated = converter.executeConversion();
  }
}
