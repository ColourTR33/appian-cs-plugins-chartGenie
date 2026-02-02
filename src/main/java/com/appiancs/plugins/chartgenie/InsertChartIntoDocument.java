package com.appiancs.plugins.chartgenie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.SmartServiceContext;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancs.plugins.chartgenie.base.BaseSmartService;
import com.appiancs.plugins.chartgenie.utils.DocumentUtils;

@PaletteInfo(paletteCategory = "Document Generation", palette = "ChartGenie Services")
public class InsertChartIntoDocument extends BaseSmartService {

  private Long chartImageId;
  private Long existingDocumentId;
  private Long targetFolderId;
  private String targetName;
  private Long updatedDocumentId;

  public InsertChartIntoDocument(SmartServiceContext context, ContentService contentService) {
    super(contentService);
  }

  @Override
  public void run() throws SmartServiceException {
    File tempImage = null;
    File tempWordInput = null;
    File resultFile = null;

    try {
      log.info("Starting InsertChartIntoDocument...");

      if (chartImageId == null)
        throw new IllegalArgumentException("Chart Image is required.");
      if (existingDocumentId == null && (targetFolderId == null || targetName == null)) {
        throw new IllegalArgumentException("If no Existing Document is provided, Target Folder and Name are required.");
      }

      Document imgDoc = contentService.download(chartImageId, ContentConstants.VERSION_CURRENT, false)[0];
      tempImage = File.createTempFile("input_chart_", ".png");
      try (InputStream in = imgDoc.getInputStream()) {
        Files.copy(in, tempImage.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      tempWordInput = File.createTempFile("temp_word_input_", ".docx");
      if (existingDocumentId != null) {
        Document wordDoc = contentService.download(existingDocumentId, ContentConstants.VERSION_CURRENT, false)[0];
        try (InputStream in = wordDoc.getInputStream()) {
          Files.copy(in, tempWordInput.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
      } else {
        try (XWPFDocument doc = new XWPFDocument(); FileOutputStream out = new FileOutputStream(tempWordInput)) {
          doc.write(out);
        }
      }

      resultFile = File.createTempFile("result_doc_", ".docx");
      insertImage(tempWordInput, tempImage, resultFile);

      // Upload using Utility
      if (existingDocumentId == null) {
        this.updatedDocumentId = DocumentUtils.uploadDocument(contentService, resultFile, targetName, targetFolderId, "docx");
      } else {
        DocumentUtils.uploadNewVersion(contentService, resultFile, existingDocumentId);
        this.updatedDocumentId = existingDocumentId;
      }

      log.info("Document updated successfully: " + updatedDocumentId);

    } catch (Exception e) {
      handleException(e, "Error inserting chart into document");
    } finally {
      if (tempImage != null)
        tempImage.delete();
      if (tempWordInput != null)
        tempWordInput.delete();
      if (resultFile != null)
        resultFile.delete();
    }
  }

  private void insertImage(File inputDoc, File imageFile, File outputDoc) throws Exception {
    try (FileInputStream fis = new FileInputStream(inputDoc);
      XWPFDocument doc = new XWPFDocument(fis);
      FileInputStream imgIs = new FileInputStream(imageFile);
      FileOutputStream fos = new FileOutputStream(outputDoc)) {

      XWPFParagraph p = doc.createParagraph();
      p.setAlignment(ParagraphAlignment.CENTER);
      XWPFRun r = p.createRun();
      if (doc.getBodyElements().size() > 1)
        r.addBreak();
      r.addPicture(imgIs, XWPFDocument.PICTURE_TYPE_PNG, imageFile.getName(), Units.toEMU(500), Units.toEMU(300));
      doc.write(fos);
    }
  }

  @Input(required = Required.ALWAYS)
  public void setChartImage(Long chartImageId) {
    this.chartImageId = chartImageId;
  }

  @Input(required = Required.OPTIONAL)
  public void setExistingDocument(Long existingDocumentId) {
    this.existingDocumentId = existingDocumentId;
  }

  @Input(required = Required.OPTIONAL)
  public void setTargetFolder(Long targetFolderId) {
    this.targetFolderId = targetFolderId;
  }

  @Input(required = Required.OPTIONAL)
  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  @Input(required = Required.OPTIONAL)
  public Long getUpdatedDocument() {
    return updatedDocumentId;
  }
}