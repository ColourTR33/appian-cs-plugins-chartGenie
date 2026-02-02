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
      System.out.println("====== DEBUG: Parsing JSON... Length: " + (jsonPayload != null ? jsonPayload.length() : "NULL"));
      Gson gson = new Gson();
      ReportRequest request = gson.fromJson(jsonPayload, ReportRequest.class);

      if (request == null) {
        System.out.println("====== DEBUG: FATAL - Request is NULL");
        throw new IllegalArgumentException("JSON payload parsed to NULL.");
      }

      if (request.getSettings() == null)
        request.setSettings(new ReportSettings());
      if (this.includeQrCode != null)
        request.getSettings().setQrCodeEnabled(this.includeQrCode);
      if (this.qrCodeUrl != null)
        request.getSettings().setQrUrl(this.qrCodeUrl);

      if (request.getSections() != null) {
        System.out.println("====== DEBUG: Sections found: " + request.getSections().size());
      }

      System.out.println("====== DEBUG: Downloading Template ID: " + templateDocumentId);
      Document appianDoc = contentService.download(templateDocumentId, ContentConstants.VERSION_CURRENT, false)[0];
      tempTemplate = File.createTempFile("genie_template_", ".docx");
      try (InputStream in = appianDoc.getInputStream()) {
        Files.copy(in, tempTemplate.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      System.out.println("====== DEBUG: Calling WordDocumentService...");
      WordDocumentService wordService = new WordDocumentService();
      finalReport = wordService.generateReport(tempTemplate, request.getSettings(), request.getSections());

      System.out.println("====== DEBUG: Generation Complete. File Size: " + finalReport.length());

      this.newDocumentId = DocumentUtils.uploadDocument(contentService, finalReport, newDocumentName, saveInFolderId, "docx");
      System.out.println("====== DEBUG: Upload Complete. ID: " + newDocumentId);

    } catch (Exception e) {
      System.out.println("====== DEBUG: EXCEPTION CAUGHT IN RUN ======");
      e.printStackTrace();
      handleException(e, "Failed to generate chart report");
    } finally {
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