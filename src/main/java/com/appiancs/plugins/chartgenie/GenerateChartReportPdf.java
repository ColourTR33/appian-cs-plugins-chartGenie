package com.appiancs.plugins.chartgenie;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import com.appiancs.plugins.chartgenie.service.DocumentUtils;
import com.appiancs.plugins.chartgenie.service.PdfConversionService;
import com.appiancs.plugins.chartgenie.service.WordDocumentService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Smart Service: Generate Chart Report (PDF)
 * <p>
 * Reuses the full ChartGenie Word generation pipeline (all chart types, tables, sidebars,
 * rich text, QR codes) and then converts the output to a PDF using a pure-Java conversion.
 * No LibreOffice or native processes are required — safe for Appian Cloud deployments.
 */
@PaletteInfo(paletteCategory = "Document Generation", palette = "ChartGenie Services")
public class GenerateChartReportPdf extends BaseSmartService {

  private String jsonPayload;
  private Long templateDocumentId;
  private String newDocumentName;
  private Long saveInFolderId;
  private Boolean includeQrCode;
  private String qrCodeUrl;
  private Long newDocumentId;

  public GenerateChartReportPdf(SmartServiceContext context, ContentService contentService) {
    super(contentService);
  }

  @Override
  public void run() throws SmartServiceException {
    File tempTemplate = null;
    File tempDocx = null;
    File tempPdf = null;

    try {
      // 1. Validate
      if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
        throw new IllegalArgumentException("JSON Payload is empty.");
      }

      Gson gson = new GsonBuilder().setLenient().create();
      ReportRequest request;
      try {
        request = gson.fromJson(jsonPayload.trim(), ReportRequest.class);
      } catch (Exception e) {
        throw new IllegalArgumentException("JSON Syntax Error: " + e.getMessage(), e);
      }

      if (request == null) {
        throw new IllegalArgumentException("Parsed request is null.");
      }

      // 2. Settings
      ReportSettings settings = request.getSettings() != null ? request.getSettings() : new ReportSettings();
      if (this.includeQrCode != null) {
        settings.setQrCodeEnabled(this.includeQrCode);
      }
      if (this.qrCodeUrl != null) {
        settings.setQrUrl(this.qrCodeUrl);
      }

      // 3. Download template
      Path safeTempDir = Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
      Document appianDoc = contentService.download(templateDocumentId, ContentConstants.VERSION_CURRENT, false)[0];
      Path tempTemplatePath = Files.createTempFile(safeTempDir, "genie_pdf_tpl_", ".docx");
      tempTemplate = tempTemplatePath.toFile();
      try (InputStream in = appianDoc.getInputStream()) {
        Files.copy(in, tempTemplatePath, StandardCopyOption.REPLACE_EXISTING);
      }

      // 4. Generate DOCX in-memory (reuse full Word pipeline)
      WordDocumentService wordService = new WordDocumentService();
      wordService.setContentService(contentService);
      byte[] docxBytes = wordService.generateReport(
        tempTemplate, settings, request.getSections(), request.getVariables());

      // 5. Write DOCX to temp file (needed for cleanup tracking only)
      Path tempDocxPath = Files.createTempFile(safeTempDir, "genie_pdf_docx_", ".docx");
      tempDocx = tempDocxPath.toFile();
      try (FileOutputStream fos = new FileOutputStream(tempDocx)) {
        fos.write(docxBytes);
      }

      // 6. Convert DOCX bytes → PDF bytes
      String pageSize = (settings.getPageSize() != null) ? settings.getPageSize() : "A4";
      PdfConversionService pdfService = new PdfConversionService();
      byte[] pdfBytes = pdfService.convert(docxBytes, pageSize);

      // 7. Write PDF to temp file and upload
      Path tempPdfPath = Files.createTempFile(safeTempDir, "genie_pdf_out_", ".pdf");
      tempPdf = tempPdfPath.toFile();
      try (FileOutputStream fos = new FileOutputStream(tempPdf)) {
        fos.write(pdfBytes);
      }

      // 8. Strip .docx/.pdf extension from name if user accidentally included it
      String safeName = sanitizePdfName(newDocumentName);
      this.newDocumentId = DocumentUtils.uploadDocument(contentService, tempPdf, safeName, saveInFolderId, "pdf");

      log.info("Successfully generated PDF report: {} with ID: {}", safeName, newDocumentId);

    } catch (IllegalArgumentException e) {
      handleException(e, e.getMessage());
    } catch (Exception e) {
      log.error("PDF report generation failed", e);
      handleException(e, "PDF Report Generation Failed");
    } finally {
      deleteSilently(tempTemplate);
      deleteSilently(tempDocx);
      deleteSilently(tempPdf);
    }
  }

  private void deleteSilently(File file) {
    if (file != null && file.exists() && !file.delete()) {
      log.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
    }
  }

  private String sanitizePdfName(String name) {
    if (name == null) {
      return "report";
    }
    // Remove any extension the user may have included
    String clean = name.replaceAll("(?i)\\.(pdf|docx|doc)$", "").trim();
    // Remove path traversal characters
    return clean.replaceAll("[.]{2,}", "").replaceAll("[/\\\\:*?\"<>|]", "").trim();
  }

  // ─── Inputs ───────────────────────────────────────────────────────────────

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

  // ─── Outputs ──────────────────────────────────────────────────────────────

  @Input(required = Required.OPTIONAL)
  public Long getNewDocument() {
    return newDocumentId;
  }
}
