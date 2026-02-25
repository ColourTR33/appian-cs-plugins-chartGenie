package com.appiancs.plugins.chartgenie;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.SmartServiceContext;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancs.plugins.chartgenie.base.BaseSmartService;
import com.appiancs.plugins.chartgenie.dto.structure.ReportRequest;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSettings;
import com.appiancs.plugins.chartgenie.service.WordDocumentService;
import com.appiancs.plugins.chartgenie.utils.DocumentUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@PaletteInfo(paletteCategory = "Document Generation", palette = "ChartGenie Services")
public class GenerateChartReport extends BaseSmartService {

  private String jsonPayload;
  private Long templateDocumentId;
  private String newDocumentName;
  private Long saveInFolderId;
  private Boolean includeQrCode;
  private String qrCodeUrl;
  private Long newDocumentId;

  public GenerateChartReport(SmartServiceContext context, ContentService contentService) {
    super(contentService);
  }

  @Override
  public void run() throws SmartServiceException {
    File tempTemplate = null;
    File finalReport = null;

    System.out.println("====== DEBUG: CHART GENIE STARTED ======");

    try {
      // 1. Pre-validation
      if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
        throw new IllegalArgumentException("The JSON Payload is empty. Please provide a valid configuration.");
      }
      if (templateDocumentId == null) {
        throw new IllegalArgumentException("Template Document ID is missing.");
      }

      String cleanJson = jsonPayload.trim();
      System.out.println("====== DEBUG: Parsing JSON... Length: " + cleanJson.length());

      // 2. Lenient Parsing
      Gson gson = new GsonBuilder().setLenient().create();
      ReportRequest request;

      try {
        request = gson.fromJson(cleanJson, ReportRequest.class);
      } catch (com.google.gson.JsonSyntaxException jse) {
        throw new IllegalArgumentException("JSON Syntax Error: " + jse.getMessage() +
          ". Tip: Check for trailing commas or unclosed brackets.");
      }

      if (request == null || request.getSections() == null || request.getSections().isEmpty()) {
        throw new IllegalArgumentException("JSON structure is valid but contains no 'sections'. Nothing to generate.");
      }

      // 3. Settings Merge
      if (request.getSettings() == null)
        request.setSettings(new ReportSettings());
      if (this.includeQrCode != null)
        request.getSettings().setQrCodeEnabled(this.includeQrCode);
      if (this.qrCodeUrl != null)
        request.getSettings().setQrUrl(this.qrCodeUrl);

      // 4. File Handling
      System.out.println("====== DEBUG: Downloading Template ID: " + templateDocumentId);
      Document appianDoc = contentService.download(templateDocumentId, ContentConstants.VERSION_CURRENT, false)[0];
      tempTemplate = File.createTempFile("genie_template_", ".docx");
      try (InputStream in = appianDoc.getInputStream()) {
        Files.copy(in, tempTemplate.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      // 5. Generation
      System.out.println("====== DEBUG: Calling WordDocumentService...");
      WordDocumentService wordService = new WordDocumentService();
      finalReport = wordService.generateReport(tempTemplate, request.getSettings(), request.getSections());

      // 6. Finalization
      System.out.println("====== DEBUG: Generation Complete. File Size: " + finalReport.length());
      this.newDocumentId = DocumentUtils.uploadDocument(contentService, finalReport, newDocumentName, saveInFolderId, "docx");
      System.out.println("====== DEBUG: Upload Complete. ID: " + newDocumentId);

    } catch (IllegalArgumentException e) {
      // Catch specific user-input errors for a clean Appian message
      System.out.println("====== DEBUG: VALIDATION ERROR: " + e.getMessage());
      handleException(e, e.getMessage());
    } catch (Exception e) {
      // Catch systemic failures
      System.out.println("====== DEBUG: EXCEPTION CAUGHT IN RUN ======");
      e.printStackTrace();
      handleException(e, "Unexpected error: " + e.getMessage());
    } finally {
      // Cleanup
      if (tempTemplate != null)
        try {
          Files.delete(tempTemplate.toPath());
        } catch (Exception ignored) {
        }
      if (finalReport != null)
        try {
          Files.delete(finalReport.toPath());
        } catch (Exception ignored) {
        }
    }
  }

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

  @Input(required = Required.OPTIONAL)
  public void setIncludeQrCode(Boolean includeQrCode) {
    this.includeQrCode = includeQrCode;
  }

  @Input(required = Required.OPTIONAL)
  public void setQrCodeUrl(String qrCodeUrl) {
    this.qrCodeUrl = qrCodeUrl;
  }

  @Input(required = Required.OPTIONAL)
  public Long getNewDocument() {
    return newDocumentId;
  }
}