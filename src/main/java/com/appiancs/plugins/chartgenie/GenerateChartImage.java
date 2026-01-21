package com.appiancs.plugins.chartgenie;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.Unattended;
import com.appiancorp.suiteapi.type.Type;
import com.appiancs.plugins.chartgenie.base.BaseSmartService;
import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.service.ChartGenerationService;

@Unattended
@Order({
  "ChartType",
  "DataCategories",
  "DataValues",
  "PrimaryColour",
  "SecondaryColour",
  "TargetFolder",
  "TargetName"
})
public class GenerateChartImage extends BaseSmartService {
  private static final Logger LOG = Logger.getLogger(GenerateChartImage.class);

  // --- Inputs ---
  private String chartType;
  private List<String> dataCategories;
  private List<Number> dataValues;
  private String primaryColour;
  private String secondaryColour;
  private Long targetFolder;
  private String targetName;

  // --- Output ---
  private Long newChartDocument;

  public GenerateChartImage(ContentService cs) {
    super(cs, LOG);
  }

  @Override
  public void run() throws SmartServiceException {
    ChartConfiguration config = new ChartConfiguration();

    // 1. Map Inputs to Configuration
    // Note: We do not pass 'cs' to config anymore, as the Service is now "Pure"
    config.setChartType(this.chartType);
    config.setDataFromLists(this.dataCategories, this.dataValues);
    config.setHexColors(Arrays.asList(this.primaryColour, this.secondaryColour));
    config.setWidth(600); // Default or add as input
    config.setHeight(400); // Default or add as input

    // 2. Call the "Pure" Service
    ChartGenerationService service = new ChartGenerationService();
    File chartFile = null;

    try {
      // This generates the file on disk (C:/Temp/...) without knowing about Appian
      chartFile = service.generateChartImage(config);

      // 3. Handle Appian Upload
      Document doc = new Document();
      doc.setName(this.targetName);
      doc.setExtension("png");
      doc.setParent(this.targetFolder);

      // Create the object in Appian
      this.newChartDocument = cs.create(doc, ContentConstants.UNIQUE_NONE);

      // Get the object back to write to its stream
      Document uploadedDoc = cs.download(this.newChartDocument, ContentConstants.VERSION_CURRENT, false)[0];

      try (OutputStream out = uploadedDoc.getOutputStream()) {
        Files.copy(chartFile.toPath(), out);
      }

    } catch (Exception e) {
      handleError(e, "Failed to generate chart image");
    } finally {
      // Cleanup temp file
      if (chartFile != null && chartFile.exists()) {
        chartFile.delete();
      }
    }
  }

  // --- Setter Methods (Appian Inputs) ---

  @Input(required = Required.ALWAYS)
  @com.appiancorp.suiteapi.common.Name("ChartType")
  @Type(name = "Text", namespace = "http://www.appian.com/ae/types/2009")
  public void setChartType(String val) {
    this.chartType = val;
  }

  @Input(required = Required.ALWAYS)
  @com.appiancorp.suiteapi.common.Name("DataCategories")
  @Type(name = "Text?list", namespace = "http://www.appian.com/ae/types/2009")
  public void setDataCategories(List<String> val) {
    this.dataCategories = val;
  }

  @Input(required = Required.ALWAYS)
  @com.appiancorp.suiteapi.common.Name("DataValues")
  @Type(name = "Number?list", namespace = "http://www.appian.com/ae/types/2009")
  public void setDataValues(List<Number> val) {
    this.dataValues = val;
  }

  @Input(required = Required.OPTIONAL)
  @com.appiancorp.suiteapi.common.Name("PrimaryColour")
  @Type(name = "Text", namespace = "http://www.appian.com/ae/types/2009")
  public void setPrimaryColour(String val) {
    this.primaryColour = val;
  }

  @Input(required = Required.OPTIONAL)
  @com.appiancorp.suiteapi.common.Name("SecondaryColour")
  @Type(name = "Text", namespace = "http://www.appian.com/ae/types/2009")
  public void setSecondaryColour(String val) {
    this.secondaryColour = val;
  }

  @Input(required = Required.ALWAYS)
  @com.appiancorp.suiteapi.common.Name("TargetFolder")
  @Type(name = "Integer", namespace = "http://www.appian.com/ae/types/2009")
  public void setTargetFolder(Long val) {
    this.targetFolder = val;
  }

  @Input(required = Required.ALWAYS)
  @com.appiancorp.suiteapi.common.Name("TargetName")
  @Type(name = "Text", namespace = "http://www.appian.com/ae/types/2009")
  public void setTargetName(String val) {
    this.targetName = val;
  }

  // --- Getter Method (Appian Output) ---

  @com.appiancorp.suiteapi.common.Name("NewChartDocument")
  public Long getNewChartDocument() {
    return newChartDocument;
  }
}