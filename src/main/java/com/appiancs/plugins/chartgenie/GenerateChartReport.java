package com.appiancs.plugins.chartgenie;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.SmartServiceContext;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancs.plugins.chartgenie.dto.structure.ReportRequest;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.appiancs.plugins.chartgenie.service.ChartGenerationService;
import com.appiancs.plugins.chartgenie.service.WordDocumentService;
import com.google.gson.Gson;

@PaletteInfo(paletteCategory = "Document Generation", palette = "ChartGenie Services")
public class GenerateChartReport extends AppianSmartService {

  private static final Logger LOG = Logger.getLogger(GenerateChartReport.class);
  private final ContentService contentService;

  // Inputs
  private String jsonPayload;
  private Long templateDocumentId;
  private String newDocumentName;
  private Long saveInFolderId;

  // Outputs
  private Long newDocumentId;
  private Boolean error = false;
  private String errorMessage = "";

  public GenerateChartReport(SmartServiceContext context, ContentService contentService) {
    super();
    this.contentService = contentService;
  }

  @Override
  public void run() throws SmartServiceException {
    File workingFile = null;
    File finalFile = null;

    try {
      LOG.info("Starting ChartGenie Report Generation...");

      // 1. Parse JSON
      Gson gson = new Gson();
      ReportRequest request = gson.fromJson(jsonPayload, ReportRequest.class);

      // 2. Instantiate Services
      WordDocumentService wordService = new WordDocumentService();
      ChartGenerationService chartService = new ChartGenerationService();

      // 3. Get Template Stream
      Document[] docs = contentService.download(templateDocumentId, ContentConstants.VERSION_CURRENT, false);
      if (docs == null || docs.length == 0) {
        throw new Exception("Template document " + templateDocumentId + " could not be found or downloaded.");
      }

      // 4. Create Base Document
      try (InputStream templateStream = docs[0].getInputStream()) {
        workingFile = wordService.createBaseDocument(templateStream);
      }

      // 5. Apply Global Headers
      if (request.getDocumentSettings() != null) {
        workingFile = wordService.applyGlobalHeaderFooter(workingFile, request.getDocumentSettings());
      }

      // 6. Loop through Sections
      for (ReportSection section : request.getContent()) {
        switch (section.getType()) {
          case SIDEBAR_LAYOUT:
            workingFile = wordService.appendSidebarLayout(
              workingFile,
              request.getDocumentSettings(),
              section.getLeftContent(),
              section.getRightContent(),
              chartService);
            break;
          case PAGE_BREAK:
            workingFile = wordService.addPageBreak(workingFile);
            break;
        }
      }
      finalFile = workingFile;

      // 7. Upload Result to Appian
      Document doc = new Document();
      doc.setName(newDocumentName);
      doc.setParent(saveInFolderId);
      doc.setExtension("docx");

      // A. Create the document shell (returns ID)
      newDocumentId = contentService.create(doc, ContentConstants.UNIQUE_NONE);

      // B. Prepare Doc object for version creation
      doc.setId(newDocumentId);

      try (FileInputStream fis = new FileInputStream(finalFile)) {

        // --- FIX: Use Reflection to set Input Stream ---
        // This bypasses the compiler error if the SDK jar is missing the method signature
        try {
          Method setInputStreamMethod = doc.getClass().getMethod("setInputStream", InputStream.class);
          setInputStreamMethod.invoke(doc, fis);
        } catch (Exception reflectEx) {
          LOG.error("Reflection failed for setInputStream: " + reflectEx.getMessage());
          throw reflectEx;
        }

        // C. Create Version (commits the content)
        // '1' = Major Version (VERSION_OPTION_MAJOR)
        contentService.createVersion(doc, 1);
      }

      LOG.info("Report Generation Successful. Doc ID: " + newDocumentId);
      this.error = false;
      this.errorMessage = null;

    } catch (Exception e) {
      LOG.error("Error in ChartGenie: " + e.getMessage(), e);
      this.error = true;
      this.errorMessage = "ChartGenie Failure: " + e.getMessage();
      this.newDocumentId = null;
    }
  }

  // --- Inputs ---

  @Input(required = Required.ALWAYS)
  public void setJsonPayload(String jsonPayload) {
    this.jsonPayload = jsonPayload;
  }

  @Input(required = Required.ALWAYS)
  public void setTemplateDocument(Long templateDocumentId) {
    this.templateDocumentId = templateDocumentId;
  }

  @Input(required = Required.ALWAYS)
  public void setNewDocumentName(String newDocumentName) {
    this.newDocumentName = newDocumentName;
  }

  @Input(required = Required.ALWAYS)
  public void setSaveInFolder(Long saveInFolderId) {
    this.saveInFolderId = saveInFolderId;
  }

  // --- Outputs ---

  @Input(required = Required.OPTIONAL)
  public Long getNewDocument() {
    return newDocumentId;
  }

  @Input(required = Required.OPTIONAL)
  public Boolean getIsError() {
    return error;
  }

  @Input(required = Required.OPTIONAL)
  public String getErrorMessage() {
    return errorMessage;
  }
}