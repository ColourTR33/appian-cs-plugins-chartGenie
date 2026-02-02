package com.appiancs.plugins.chartgenie.service;

import java.io.File;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;

/**
 * "Cold Handover" Uploader.
 * Avoids file locks to ensure Appian can read the data from disk.
 */
public class AppianDocumentUploader {

  private static final Logger LOG = Logger.getLogger(AppianDocumentUploader.class);
  private final ContentService contentService;

  public AppianDocumentUploader(ContentService contentService) {
    this.contentService = contentService;
  }

  public Long uploadNewDocument(File file, String name, Long folderId, String extension) throws Exception {
    LOG.info("Initiating Cold Handover for: " + name + "." + extension + " (" + file.length() + " bytes)");

    Document doc = new Document();
    doc.setName(name);
    doc.setExtension(extension);
    doc.setParent(folderId);
    doc.setSize((int) file.length());

    // CRITICAL FIX 1: Set FileSystemId
    // Without this, Appian creates a database row but allocates no storage space.
    // We try 'ALLOCATE_FSID' (usually 1).
    try {
      doc.setFileSystemId(ContentConstants.ALLOCATE_FSID);
    } catch (Exception e) {
      // Reflection fallback if constant is missing
      try {
        Method setFs = doc.getClass().getMethod("setFileSystemId", Integer.class);
        setFs.invoke(doc, 1);
      } catch (Exception ignored) {
      }
    }

    // CRITICAL FIX 2: Set Internal Filename (The "Handover")
    // We do NOT open a stream here. We just point to the path.
    try {
      Method setFilename = doc.getClass().getMethod("setInternalFilename", String.class);
      setFilename.invoke(doc, file.getAbsolutePath());
    } catch (Exception e) {
      LOG.warn("Could not set internal filename.", e);
    }

    // 1. Create Metadata Shell
    // If 'setInternalFilename' worked, 'create' might upload the data automatically.
    Long newId = contentService.create(doc, ContentConstants.UNIQUE_NONE);
    doc.setId(newId);
    LOG.info("Shell created. ID: " + newId);

    // 2. Force Content Push (Double Tap)
    // We call 'uploadDocument' to ensure the data is moved.
    pushContent(doc);

    return newId;
  }

  public void uploadNewVersion(File file, Long docId) throws Exception {
    LOG.info("Initiating Version Handover for ID: " + docId);

    Document doc = new Document();
    doc.setId(docId);
    doc.setSize((int) file.length());

    try {
      Method setFilename = doc.getClass().getMethod("setInternalFilename", String.class);
      setFilename.invoke(doc, file.getAbsolutePath());
    } catch (Exception ignored) {
    }

    // Try 'createVersion' first
    try {
      Method createVersion = ContentService.class.getMethod("createVersion",
        com.appiancorp.suiteapi.content.Content.class, Integer.class);
      createVersion.invoke(contentService, doc, ContentConstants.UNIQUE_NONE);
      LOG.info("Version created.");
    } catch (Exception e) {
      // Fallback to push
      pushContent(doc);
    }
  }

  private void pushContent(Document doc) {
    boolean success = false;

    // Try 'uploadDocument'
    try {
      Method uploadDoc = ContentService.class.getMethod("uploadDocument",
        com.appiancorp.suiteapi.knowledge.Document.class, Integer.class);
      uploadDoc.invoke(contentService, doc, ContentConstants.UNIQUE_NONE);
      success = true;
      LOG.info("Pushed via 'uploadDocument'.");
    } catch (Exception ignored) {
    }

    // Try 'upload'
    if (!success) {
      try {
        Method upload = ContentService.class.getMethod("upload",
          com.appiancorp.suiteapi.knowledge.Document.class, Integer.class);
        upload.invoke(contentService, doc, ContentConstants.UNIQUE_NONE);
        success = true;
        LOG.info("Pushed via 'upload'.");
      } catch (Exception ignored) {
      }
    }

    if (!success) {
      LOG.warn("WARNING: No explicit upload method succeeded. Reliance is purely on 'create' + 'setInternalFilename'.");
    }
  }
}