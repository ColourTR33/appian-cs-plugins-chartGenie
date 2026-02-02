package com.appiancs.plugins.chartgenie.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;

/**
 * Standard utility for Appian Document I/O.
 * Uses the "Re-Fetch" strategy to ensure a valid stream connection.
 */
public final class DocumentUtils {

  private static final Logger LOG = Logger.getLogger(DocumentUtils.class);

  private DocumentUtils() {
    // Utility class
  }

  public static Long uploadDocument(ContentService contentService, File file, String name, Long folderId, String extension)
    throws Exception {
    LOG.info("Uploading: " + name + "." + extension + " (" + file.length() + " bytes)");

    // 1. Create Metadata Shell
    Document shell = new Document();
    shell.setName(name);
    shell.setExtension(extension);
    shell.setParent(folderId);
    shell.setSize((int) file.length());

    Long docId = contentService.create(shell, ContentConstants.UNIQUE_NONE);
    LOG.info("Shell created with ID: " + docId);

    // 2. Stream Content into the Live Object
    uploadContentToExistingDoc(contentService, docId, file);

    return docId;
  }

  public static void uploadNewVersion(ContentService contentService, File file, Long docId) throws Exception {
    LOG.info("Uploading new version for ID: " + docId);

    // Stream data first
    uploadContentToExistingDoc(contentService, docId, file);

    // Explicitly increment version if required by server
    try {
      Document verDoc = new Document();
      verDoc.setId(docId);
      contentService.createVersion(verDoc, ContentConstants.UNIQUE_NONE);
    } catch (Exception e) {
      LOG.debug("createVersion call skipped/failed (content might already be updated): " + e.getMessage());
    }
  }

  private static void uploadContentToExistingDoc(ContentService contentService, Long docId, File file) throws Exception {
    // A. Re-Fetch the LIVE object
    Document liveDoc = contentService.download(docId, ContentConstants.VERSION_CURRENT, false)[0];

    // B. Copy Bytes (Standard I/O)
    try (InputStream fileIn = new FileInputStream(file);
      OutputStream docOut = liveDoc.getOutputStream()) {

      if (docOut == null) {
        throw new IllegalStateException("FATAL: Appian Document returned null OutputStream.");
      }

      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = fileIn.read(buffer)) != -1) {
        docOut.write(buffer, 0, bytesRead);
      }

      docOut.flush();
      LOG.info("Content stream completed successfully.");
    }
  }
}