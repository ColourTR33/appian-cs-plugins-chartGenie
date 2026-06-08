package com.appiancs.plugins.chartgenie.service;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;

/**
 * "Cold Handover" Uploader.
 * Avoids file locks to ensure Appian can read the data from disk.
 */
public class AppianDocumentUploader {

  // FIXED: Correct SLF4J initialization
  private static final Logger LOG = LoggerFactory.getLogger(AppianDocumentUploader.class);
  private final ContentService contentService;

  public AppianDocumentUploader(ContentService contentService) {
    this.contentService = contentService;
  }

  public Long uploadNewDocument(File file, String name, Long folderId, String extension) throws Exception {
    LOG.info("Initiating Cold Handover for: {}.{} ({} bytes)", name, extension, file.length());

    // CWE-22/23: resolve canonical path before any use
    Path safePath = file.toPath().normalize().toRealPath();

    Document doc = new Document();
    doc.setName(name);
    doc.setExtension(extension);
    doc.setParent(folderId);
    doc.setSize((int) file.length());

    try {
      doc.setFileSystemId(ContentConstants.ALLOCATE_FSID);
    } catch (Exception e) {
      LOG.warn("Reflection fallback failed for setFileSystemId", e);
      try {
        Method setFs = doc.getClass().getMethod("setFileSystemId", Integer.class);
        setFs.invoke(doc, 1);
      } catch (Exception reflectionException) {
        LOG.debug("Reflection fallback also failed", reflectionException);
      }
    }

    try {
      Method setFilename = doc.getClass().getMethod("setInternalFilename", String.class);
      setFilename.invoke(doc, safePath.toString());
    } catch (Exception e) {
      LOG.debug("setInternalFilename method not available or failed", e);
    }

    Long newId = contentService.create(doc, ContentConstants.UNIQUE_NONE);
    doc.setId(newId);
    LOG.info("Shell created. ID: {}", newId);

    pushContent(doc);

    return newId;
  }

  public void uploadNewVersion(File file, Long docId) throws Exception {
    LOG.info("Initiating Version Handover for ID: {}", docId);

    // CWE-22/23: resolve canonical path before any use
    Path safePath = file.toPath().normalize().toRealPath();

    Document doc = new Document();
    doc.setId(docId);
    doc.setSize((int) file.length());

    try {
      Method setFilename = doc.getClass().getMethod("setInternalFilename", String.class);
      setFilename.invoke(doc, safePath.toString());
    } catch (Exception e) {
      LOG.debug("setInternalFilename method not available for version update", e);
    }

    try {
      Method createVersion = ContentService.class.getMethod("createVersion",
        com.appiancorp.suiteapi.content.Content.class, Integer.class);
      createVersion.invoke(contentService, doc, ContentConstants.UNIQUE_NONE);
      LOG.info("Version created.");
    } catch (Exception e) {
      LOG.warn("createVersion method failed, falling back to pushContent", e);
      pushContent(doc);
    }
  }

  private void pushContent(Document doc) {
    boolean success = false;

    try {
      Method uploadDoc = ContentService.class.getMethod("uploadDocument",
        com.appiancorp.suiteapi.knowledge.Document.class, Integer.class);
      uploadDoc.invoke(contentService, doc, ContentConstants.UNIQUE_NONE);
      success = true;
      LOG.info("Pushed via 'uploadDocument'.");
    } catch (Exception e) {
      LOG.debug("uploadDocument method failed", e);
    }

    if (!success) {
      try {
        Method upload = ContentService.class.getMethod("upload",
          com.appiancorp.suiteapi.knowledge.Document.class, Integer.class);
        upload.invoke(contentService, doc, ContentConstants.UNIQUE_NONE);
        success = true;
        LOG.info("Pushed via 'upload'.");
      } catch (Exception e) {
        LOG.debug("upload method failed", e);
      }
    }

    if (!success) {
      LOG.warn("No explicit upload method succeeded. Reliance is purely on 'create' + 'setInternalFilename'.");
    }
  }
}