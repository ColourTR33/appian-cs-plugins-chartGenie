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

    try {
      // 1. Validation & Lenient Parsing
      if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
        throw new IllegalArgumentException("JSON Payload is empty.");
      }

      Gson gson = new GsonBuilder().setLenient().create();
      ReportRequest request;
      try {
        request = gson.fromJson(jsonPayload.trim(), ReportRequest.class);
      } catch (Exception e) {
        throw new IllegalArgumentException("JSON Syntax Error: " + e.getMessage());
      }

      if (request == null)
        throw new IllegalArgumentException("Parsed request is null.");

      // 2. Settings Prep
      ReportSettings settings = request.getSettings() != null ? request.getSettings() : new ReportSettings();
      if (this.includeQrCode != null)
        settings.setQrCodeEnabled(this.includeQrCode);
      if (this.qrCodeUrl != null)
        settings.setQrUrl(this.qrCodeUrl);

      // 3. Template Download
      Document appianDoc = contentService.download(templateDocumentId, ContentConstants.VERSION_CURRENT, false)[0];
      tempTemplate = File.createTempFile("genie_tpl_", ".docx");
      try (InputStream in = appianDoc.getInputStream()) {
        Files.copy(in, tempTemplate.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      // 4. Generation
      WordDocumentService wordService = new WordDocumentService();
      finalReport = wordService.generateReport(tempTemplate, settings, request.getSections());

      // 5. Upload
      this.newDocumentId = DocumentUtils.uploadDocument(contentService, finalReport, newDocumentName, saveInFolderId, "docx");

    } catch (Exception e) {
      handleException(e, e instanceof IllegalArgumentException ? e.getMessage() : "Report Generation Failed");
    } finally {
      if (tempTemplate != null)
        tempTemplate.delete();
      if (finalReport != null)
        finalReport.delete();
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