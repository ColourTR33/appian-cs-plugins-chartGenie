package com.appiancs.plugins.chartgenie;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
import com.appiancs.plugins.chartgenie.service.WordDocumentService;

@Unattended
@Order({
  "ChartImage",
  "ExistingDocument",
  "TargetFolder",
  "TargetName"
})
public class InsertChartIntoDocument extends BaseSmartService {
  private static final Logger LOG = Logger.getLogger(InsertChartIntoDocument.class);

  // --- Inputs ---
  private Long chartImageId;
  private Long existingDocumentId; // Optional: If present, we append. If null, we create.
  private Long targetFolder; // Only needed if creating new
  private String targetName; // Only needed if creating new

  // --- Output ---
  private Long updatedDocumentId;

  public InsertChartIntoDocument(ContentService cs) {
    super(cs, LOG);
  }

  @Override
  public void run() throws SmartServiceException {
    WordDocumentService service = new WordDocumentService();
    File tempImage = null;
    File tempWordInput = null;
    File resultFile = null;

    try {
      // 1. Download Input Image (From Appian to Temp File)
      Document imgDoc = cs.download(chartImageId, ContentConstants.VERSION_CURRENT, false)[0];
      tempImage = File.createTempFile("input_chart_", ".png");

      try (InputStream in = imgDoc.getInputStream()) {
        Files.copy(in, tempImage.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      // 2. Execute Logic (Existing vs New)
      if (existingDocumentId != null) {
        // CASE A: Append to Existing Document
        Document wordDoc = cs.download(existingDocumentId, ContentConstants.VERSION_CURRENT, false)[0];
        tempWordInput = File.createTempFile("temp_input_doc_", ".docx");

        try (InputStream in = wordDoc.getInputStream()) {
          Files.copy(in, tempWordInput.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Call Pure Service
        resultFile = service.appendChartToDocument(tempWordInput, tempImage);

      } else {
        // CASE B: Create New Document
        // Call Pure Service
        resultFile = service.createDocumentWithChart(tempImage);
      }

      // 3. Upload Result (The Plumbing)
      if (existingDocumentId == null) {
        // Upload as NEW Document
        Document newDoc = new Document();
        newDoc.setName(targetName);
        newDoc.setExtension("docx");
        newDoc.setParent(targetFolder);

        this.updatedDocumentId = cs.create(newDoc, ContentConstants.UNIQUE_NONE);

        Document finalDoc = cs.download(this.updatedDocumentId, ContentConstants.VERSION_CURRENT, false)[0];
        try (OutputStream out = finalDoc.getOutputStream()) {
          Files.copy(resultFile.toPath(), out);
        }

      } else {
        // Upload as NEW VERSION of Existing Document
        Document versionDoc = cs.download(existingDocumentId, ContentConstants.VERSION_CURRENT, false)[0];

        // Getting OutputStream on an existing doc automatically creates a new version in Appian
        try (OutputStream out = versionDoc.getOutputStream()) {
          Files.copy(resultFile.toPath(), out);
        }
        this.updatedDocumentId = existingDocumentId;
      }

    } catch (Exception e) {
      handleError(e, "Error inserting chart into document");
    } finally {
      // Cleanup
      deleteQuietly(tempImage);
      deleteQuietly(tempWordInput);
      deleteQuietly(resultFile);
    }
  }

  private void deleteQuietly(File f) {
    if (f != null && f.exists()) {
      f.delete();
    }
  }

  // --- Setter Methods (Appian Inputs) ---

  @Input(required = Required.ALWAYS)
  @com.appiancorp.suiteapi.common.Name("ChartImage")
  @Type(name = "Document", namespace = "http://www.appian.com/ae/types/2009")
  public void setChartImage(Long val) {
    this.chartImageId = val;
  }

  @Input(required = Required.OPTIONAL)
  @com.appiancorp.suiteapi.common.Name("ExistingDocument")
  @Type(name = "Document", namespace = "http://www.appian.com/ae/types/2009")
  public void setExistingDocument(Long val) {
    this.existingDocumentId = val;
  }

  @Input(required = Required.OPTIONAL)
  @com.appiancorp.suiteapi.common.Name("TargetFolder")
  @Type(name = "Integer", namespace = "http://www.appian.com/ae/types/2009")
  public void setTargetFolder(Long val) {
    this.targetFolder = val;
  }

  @Input(required = Required.OPTIONAL)
  @com.appiancorp.suiteapi.common.Name("TargetName")
  @Type(name = "Text", namespace = "http://www.appian.com/ae/types/2009")
  public void setTargetName(String val) {
    this.targetName = val;
  }

  // --- Getter Method (Appian Output) ---

  @com.appiancorp.suiteapi.common.Name("UpdatedDocument")
  public Long getUpdatedDocument() {
    return updatedDocumentId;
  }
}